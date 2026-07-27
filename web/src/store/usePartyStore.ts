import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  runTransaction,
  setDoc,
  updateDoc,
} from 'firebase/firestore'
import { db } from '../firebase'
import type {
  CategoryEntity,
  EventEntity,
  ExpenseEntity,
  FinancialSummary,
  ParticipantEntity,
} from '../types'
import { DEFAULT_CATEGORIES, DEFINE_LATER_CHILD_WEIGHT } from '../types'

export function calculateParticipantTarget(
  participant: ParticipantEntity,
  event: EventEntity,
  allParticipants: ParticipantEntity[],
): number {
  switch (event.costShareMode) {
    case 'ORGANIZER_ONLY':
      return 0
    case 'FIXED_TYPE':
      return participant.type === 'ADULT' ? event.fixedAdultPrice : event.fixedChildPrice
    case 'EQUAL':
      return allParticipants.length > 0 ? event.budget / allParticipants.length : 0
    case 'DEFINE_LATER': {
      const adultCount = allParticipants.filter((p) => p.type === 'ADULT').length
      const childCount = allParticipants.filter((p) => p.type === 'CHILD').length
      const totalWeight = adultCount + childCount * DEFINE_LATER_CHILD_WEIGHT
      if (totalWeight <= 0) return 0
      const unit = event.budget / totalWeight
      return participant.type === 'ADULT' ? unit : unit * DEFINE_LATER_CHILD_WEIGHT
    }
  }
}

export function calculateDefineLaterSuggestion(
  event: EventEntity,
  allParticipants: ParticipantEntity[],
): { adultPrice: number; childPrice: number } {
  const adultCount = allParticipants.filter((p) => p.type === 'ADULT').length
  const childCount = allParticipants.filter((p) => p.type === 'CHILD').length
  const totalWeight = adultCount + childCount * DEFINE_LATER_CHILD_WEIGHT
  if (totalWeight <= 0) return { adultPrice: 0, childPrice: 0 }
  const unit = event.budget / totalWeight
  return { adultPrice: unit, childPrice: unit * DEFINE_LATER_CHILD_WEIGHT }
}

function calculateFinancialSummary(
  event: EventEntity,
  participants: ParticipantEntity[],
  expenses: ExpenseEntity[],
): FinancialSummary {
  const totalSpent = expenses.reduce((sum, e) => sum + e.amount, 0)
  const totalPurchased = expenses.filter((e) => e.isPurchased).reduce((sum, e) => sum + e.amount, 0)
  const budgetBalance = event.budget - totalSpent

  const adultCount = participants.filter((p) => p.type === 'ADULT').length
  const childCount = participants.filter((p) => p.type === 'CHILD').length
  const totalCount = participants.length

  const totalExpected = participants.reduce(
    (sum, p) => sum + calculateParticipantTarget(p, event, participants),
    0,
  )
  const totalCollected = participants.reduce((sum, p) => sum + p.paidAmount, 0)
  const missing = Math.max(0, totalExpected - totalCollected)
  const netBalance = totalCollected - totalSpent

  return {
    budget: event.budget,
    totalSpent,
    totalPurchased,
    budgetBalance,
    totalExpectedCollection: totalExpected,
    totalCollected,
    missingCollection: missing,
    netBalance,
    adultCount,
    childCount,
    totalParticipants: totalCount,
  }
}

function emptySummary(): FinancialSummary {
  return {
    budget: 0,
    totalSpent: 0,
    totalPurchased: 0,
    budgetBalance: 0,
    totalExpectedCollection: 0,
    totalCollected: 0,
    missingCollection: 0,
    netBalance: 0,
    adultCount: 0,
    childCount: 0,
    totalParticipants: 0,
  }
}

export function usePartyStore(uid: string) {
  const [events, setEvents] = useState<EventEntity[]>([])
  const [allParticipants, setAllParticipants] = useState<ParticipantEntity[]>([])
  const [allExpenses, setAllExpenses] = useState<ExpenseEntity[]>([])
  const [categories, setCategories] = useState<CategoryEntity[]>([])
  const [activeEventId, setActiveEventId] = useState<string | null>(null)

  useEffect(() => {
    const unsubEvents = onSnapshot(collection(db, 'users', uid, 'events'), (snap) => {
      setEvents(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<EventEntity, 'id'>) })))
    })
    const unsubParticipants = onSnapshot(collection(db, 'users', uid, 'participants'), (snap) => {
      setAllParticipants(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<ParticipantEntity, 'id'>) })))
    })
    const unsubExpenses = onSnapshot(collection(db, 'users', uid, 'expenses'), (snap) => {
      setAllExpenses(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<ExpenseEntity, 'id'>) })))
    })
    const unsubCategories = onSnapshot(collection(db, 'users', uid, 'categories'), (snap) => {
      setCategories(snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<CategoryEntity, 'id'>) })))
    })
    const unsubUser = onSnapshot(doc(db, 'users', uid), (snap) => {
      const data = snap.data() as { activeEventId?: string } | undefined
      setActiveEventId(data?.activeEventId ?? null)
    })

    return () => {
      unsubEvents()
      unsubParticipants()
      unsubExpenses()
      unsubCategories()
      unsubUser()
    }
  }, [uid])

  useEffect(() => {
    // Uses a sentinel doc + transaction so this is safe even if this effect fires more than
    // once (e.g. React StrictMode double-invoking effects, or multiple tabs open at once) —
    // only the first caller to see the sentinel missing gets to seed the defaults.
    const seedSentinelRef = doc(db, 'users', uid, 'meta', 'categoriesSeeded')
    runTransaction(db, async (tx) => {
      const sentinel = await tx.get(seedSentinelRef)
      if (sentinel.exists()) return
      tx.set(seedSentinelRef, { seededAt: Date.now() })
      for (const category of DEFAULT_CATEGORIES) {
        const newCategoryRef = doc(collection(db, 'users', uid, 'categories'))
        tx.set(newCategoryRef, category)
      }
    }).catch(() => {
      // Best-effort seeding; if it fails the user can still add categories manually.
    })
  }, [uid])

  const activeEvent = useMemo(
    () => events.find((e) => e.id === activeEventId) ?? null,
    [events, activeEventId],
  )

  const participants = useMemo(
    () => (activeEvent ? allParticipants.filter((p) => p.eventId === activeEvent.id) : []),
    [allParticipants, activeEvent],
  )

  const expenses = useMemo(
    () => (activeEvent ? allExpenses.filter((e) => e.eventId === activeEvent.id) : []),
    [allExpenses, activeEvent],
  )

  const financialSummary = useMemo<FinancialSummary>(
    () => (activeEvent ? calculateFinancialSummary(activeEvent, participants, expenses) : emptySummary()),
    [activeEvent, participants, expenses],
  )

  const addEvent = useCallback(
    async (event: Omit<EventEntity, 'id'>) => {
      const ref = await addDoc(collection(db, 'users', uid, 'events'), event)
      await setDoc(doc(db, 'users', uid), { activeEventId: ref.id }, { merge: true })
    },
    [uid],
  )

  const updateEvent = useCallback(
    async (event: EventEntity) => {
      const { id, ...rest } = event
      await updateDoc(doc(db, 'users', uid, 'events', id), rest)
    },
    [uid],
  )

  const selectActiveEvent = useCallback(
    async (eventId: string) => {
      await setDoc(doc(db, 'users', uid), { activeEventId: eventId }, { merge: true })
    },
    [uid],
  )

  const deleteEvent = useCallback(
    async (event: EventEntity) => {
      const relatedParticipants = allParticipants.filter((p) => p.eventId === event.id)
      const relatedExpenses = allExpenses.filter((e) => e.eventId === event.id)

      await Promise.all([
        ...relatedParticipants.map((p) => deleteDoc(doc(db, 'users', uid, 'participants', p.id))),
        ...relatedExpenses.map((e) => deleteDoc(doc(db, 'users', uid, 'expenses', e.id))),
        deleteDoc(doc(db, 'users', uid, 'events', event.id)),
      ])

      if (activeEventId === event.id) {
        const remaining = events.filter((e) => e.id !== event.id)
        await setDoc(doc(db, 'users', uid), { activeEventId: remaining[0]?.id ?? null }, { merge: true })
      }
    },
    [uid, allParticipants, allExpenses, activeEventId, events],
  )

  const addParticipant = useCallback(
    async (participant: Omit<ParticipantEntity, 'id'>) => {
      await addDoc(collection(db, 'users', uid, 'participants'), participant)
    },
    [uid],
  )

  const updateParticipant = useCallback(
    async (participant: ParticipantEntity) => {
      const { id, ...rest } = participant
      await updateDoc(doc(db, 'users', uid, 'participants', id), rest)
    },
    [uid],
  )

  const updateParticipantPayment = useCallback(
    async (participantId: string, paidAmount: number) => {
      await updateDoc(doc(db, 'users', uid, 'participants', participantId), { paidAmount })
    },
    [uid],
  )

  const deleteParticipant = useCallback(
    async (participant: ParticipantEntity) => {
      await deleteDoc(doc(db, 'users', uid, 'participants', participant.id))
    },
    [uid],
  )

  const addExpense = useCallback(
    async (expense: Omit<ExpenseEntity, 'id'>) => {
      await addDoc(collection(db, 'users', uid, 'expenses'), expense)
    },
    [uid],
  )

  const updateExpense = useCallback(
    async (expense: ExpenseEntity) => {
      const { id, ...rest } = expense
      await updateDoc(doc(db, 'users', uid, 'expenses', id), rest)
    },
    [uid],
  )

  const toggleExpensePurchased = useCallback(
    async (expenseId: string, isPurchased: boolean) => {
      await updateDoc(doc(db, 'users', uid, 'expenses', expenseId), { isPurchased })
    },
    [uid],
  )

  const toggleExpensePaid = useCallback(
    async (expenseId: string, isPaid: boolean) => {
      await updateDoc(doc(db, 'users', uid, 'expenses', expenseId), { isPaid })
    },
    [uid],
  )

  const deleteExpense = useCallback(
    async (expense: ExpenseEntity) => {
      await deleteDoc(doc(db, 'users', uid, 'expenses', expense.id))
    },
    [uid],
  )

  const addCategory = useCallback(
    async (category: Omit<CategoryEntity, 'id'>) => {
      await addDoc(collection(db, 'users', uid, 'categories'), category)
    },
    [uid],
  )

  const updateCategory = useCallback(
    async (category: CategoryEntity) => {
      const { id, ...rest } = category
      await updateDoc(doc(db, 'users', uid, 'categories', id), rest)
    },
    [uid],
  )

  const deleteCategory = useCallback(
    async (category: CategoryEntity) => {
      await deleteDoc(doc(db, 'users', uid, 'categories', category.id))
    },
    [uid],
  )

  return {
    events,
    activeEvent,
    participants,
    expenses,
    categories,
    financialSummary,
    addEvent,
    updateEvent,
    selectActiveEvent,
    deleteEvent,
    addParticipant,
    updateParticipant,
    updateParticipantPayment,
    deleteParticipant,
    addExpense,
    updateExpense,
    toggleExpensePurchased,
    toggleExpensePaid,
    deleteExpense,
    addCategory,
    updateCategory,
    deleteCategory,
  }
}

export type PartyStore = ReturnType<typeof usePartyStore>
