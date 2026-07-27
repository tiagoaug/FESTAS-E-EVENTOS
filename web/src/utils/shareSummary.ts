import { calculateParticipantTarget } from '../store/usePartyStore'
import { FALLBACK_CATEGORY_LABEL } from '../types'
import type { CategoryEntity, EventEntity, ExpenseEntity, FinancialSummary, ParticipantEntity } from '../types'
import { formatCurrency, formatDate } from './format'

export interface PaymentSplit {
  paid: ParticipantEntity[]
  pending: ParticipantEntity[]
}

export interface ShareOptions {
  groupByFamily: boolean
  alphabetical: boolean
  showTotals: boolean
  showGuestList: boolean
  showAttendanceList: boolean
  showExpenseItems: boolean
  showBudget: boolean
  showCountdown: boolean
  notes: string
}

export const DEFAULT_SHARE_OPTIONS: ShareOptions = {
  groupByFamily: false,
  alphabetical: true,
  showTotals: true,
  showGuestList: true,
  showAttendanceList: false,
  showExpenseItems: false,
  showBudget: true,
  showCountdown: true,
  notes: '',
}

export const FULL_PARTY_DATA_OPTIONS: Partial<ShareOptions> = {
  showTotals: true,
  showGuestList: true,
  showAttendanceList: true,
  showBudget: true,
  showExpenseItems: true,
  showCountdown: true,
}

export interface AttendanceSplit {
  confirmed: ParticipantEntity[]
  awaiting: ParticipantEntity[]
}

export function splitParticipantsByAttendance(participants: ParticipantEntity[]): AttendanceSplit {
  return {
    confirmed: participants.filter((p) => p.confirmed),
    awaiting: participants.filter((p) => !p.confirmed),
  }
}

export interface ExpenseSplit {
  paidExpenses: ExpenseEntity[]
  pendingExpenses: ExpenseEntity[]
}

export function splitExpensesByPayment(expenses: ExpenseEntity[]): ExpenseSplit {
  return {
    paidExpenses: expenses.filter((e) => e.isPaid),
    pendingExpenses: expenses.filter((e) => !e.isPaid),
  }
}

export interface GuestGroup {
  family: string | null
  members: ParticipantEntity[]
}

export function groupParticipants(
  participants: ParticipantEntity[],
  groupByFamily: boolean,
  alphabetical: boolean,
): GuestGroup[] {
  const sorted = [...participants].sort((a, b) =>
    alphabetical ? a.name.localeCompare(b.name, 'pt-BR') : 0,
  )

  if (!groupByFamily) {
    return [{ family: null, members: sorted }]
  }

  const familyNames = Array.from(new Set(sorted.map((p) => p.familyGroup || 'Sem Família')))
  const orderedFamilies = alphabetical
    ? [...familyNames].sort((a, b) => a.localeCompare(b, 'pt-BR'))
    : familyNames

  return orderedFamilies.map((family) => ({
    family,
    members: sorted.filter((p) => (p.familyGroup || 'Sem Família') === family),
  }))
}

export function splitParticipantsByPayment(
  event: EventEntity,
  participants: ParticipantEntity[],
): PaymentSplit {
  const paid: ParticipantEntity[] = []
  const pending: ParticipantEntity[] = []

  for (const p of participants) {
    const target = calculateParticipantTarget(p, event, participants)
    const isPaidFull = target > 0 && p.paidAmount >= target
    if (isPaidFull) {
      paid.push(p)
    } else {
      pending.push(p)
    }
  }

  return { paid, pending }
}

export function daysUntilEvent(eventDateMillis: number): number {
  const diff = eventDateMillis - Date.now()
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
}

export function buildShareSummaryText(
  event: EventEntity,
  participants: ParticipantEntity[],
  summary: FinancialSummary,
  options: ShareOptions,
  expenses: ExpenseEntity[] = [],
  categories: CategoryEntity[] = [],
): string {
  const { paid, pending } = splitParticipantsByPayment(event, participants)
  const groups = groupParticipants(participants, options.groupByFamily, options.alphabetical)
  const lines: string[] = []

  lines.push(`🎉 ${event.title}`)
  lines.push(`📅 ${formatDate(event.eventDateMillis)}`)
  if (event.location) lines.push(`📍 ${event.location}`)

  if (options.showCountdown) {
    const days = daysUntilEvent(event.eventDateMillis)
    lines.push(`⏰ ${days > 0 ? `Faltam ${days} dia(s)` : days === 0 ? 'É hoje!' : 'Evento já realizado'}`)
  }
  lines.push('')

  if (options.showTotals) {
    lines.push('📊 TOTAIS')
    lines.push(`Convidados: ${participants.length}`)
    lines.push(`Confirmados: ${participants.filter((p) => p.confirmed).length}`)
    if (event.costShareMode !== 'ORGANIZER_ONLY') {
      lines.push(`Pagaram: ${paid.length}`)
      lines.push(`Faltam Pagar: ${pending.length}`)
    }
    lines.push('')
  }

  if (options.showBudget) {
    lines.push('💰 RESUMO FINANCEIRO')
    lines.push(`Orçamento: ${formatCurrency(summary.budget)}`)
    lines.push(`Total Gasto: ${formatCurrency(summary.totalSpent)}`)
    lines.push(`Saldo: ${formatCurrency(summary.budget - summary.totalSpent)}`)
    if (event.costShareMode !== 'ORGANIZER_ONLY') {
      lines.push(`Arrecadado: ${formatCurrency(summary.totalCollected)}`)
      lines.push(`Falta Arrecadar: ${formatCurrency(summary.missingCollection)}`)
    }
    lines.push('')
  }

  if (options.showGuestList) {
    lines.push(`👥 CONVIDADOS (${participants.length})`)
    if (participants.length === 0) {
      lines.push('Nenhum convidado cadastrado ainda.')
    } else {
      for (const group of groups) {
        if (group.family) lines.push(`— ${group.family} —`)
        for (const p of group.members) {
          lines.push(`- ${p.name}${p.confirmed ? ' ✅' : ''}`)
        }
      }
    }
    lines.push('')
  }

  if (options.showAttendanceList) {
    const { confirmed, awaiting } = splitParticipantsByAttendance(participants)
    lines.push(`✅ PRESENÇA CONFIRMADA (${confirmed.length})`)
    if (confirmed.length === 0) {
      lines.push('Ninguém confirmou presença ainda.')
    } else {
      for (const p of confirmed) lines.push(`- ${p.name}`)
    }
    lines.push('')

    lines.push(`❔ AGUARDANDO CONFIRMAÇÃO (${awaiting.length})`)
    if (awaiting.length === 0) {
      lines.push('Todos já confirmaram presença!')
    } else {
      for (const p of awaiting) lines.push(`- ${p.name}`)
    }
    lines.push('')
  }

  if (event.costShareMode === 'ORGANIZER_ONLY') {
    lines.push('ℹ️ O organizador assume o custo total — não há rateio entre os convidados.')
  } else {
    lines.push(`✅ JÁ PAGARAM (${paid.length})`)
    if (paid.length === 0) {
      lines.push('Ninguém quitou o valor ainda.')
    } else {
      for (const p of paid) {
        lines.push(`- ${p.name}: ${formatCurrency(p.paidAmount)}`)
      }
    }
    lines.push('')

    lines.push(`⏳ FALTAM PAGAR (${pending.length})`)
    if (pending.length === 0) {
      lines.push('Todo mundo está em dia!')
    } else {
      for (const p of pending) {
        const target = calculateParticipantTarget(p, event, participants)
        const missing = Math.max(0, target - p.paidAmount)
        lines.push(`- ${p.name}: falta ${formatCurrency(missing)}`)
      }
    }
    lines.push('')
  }

  if (options.showExpenseItems) {
    const { paidExpenses, pendingExpenses } = splitExpensesByPayment(expenses)
    const describeExpense = (e: ExpenseEntity) => {
      const category = categories.find((c) => c.id === e.category)
      return `- ${e.title} (${category?.name ?? FALLBACK_CATEGORY_LABEL}): ${formatCurrency(e.amount)}`
    }

    lines.push(`✅ ITENS PAGOS (${paidExpenses.length})`)
    if (paidExpenses.length === 0) {
      lines.push('Nenhum item pago ainda.')
    } else {
      for (const e of paidExpenses) lines.push(describeExpense(e))
    }
    lines.push('')

    lines.push(`⏳ ITENS A PAGAR (${pendingExpenses.length})`)
    if (pendingExpenses.length === 0) {
      lines.push('Nenhum item pendente!')
    } else {
      for (const e of pendingExpenses) lines.push(describeExpense(e))
    }
    lines.push('')
  }

  if (options.notes.trim()) {
    lines.push('📝 OBSERVAÇÕES')
    lines.push(options.notes.trim())
    lines.push('')
  }

  return lines.join('\n').trim()
}
