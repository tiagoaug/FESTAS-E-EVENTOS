export type CostShareMode = 'EQUAL' | 'FIXED_TYPE' | 'ORGANIZER_ONLY' | 'DEFINE_LATER'

/** Crianças pagam esta fração do valor do adulto no modo "Definir Depois". */
export const DEFINE_LATER_CHILD_WEIGHT = 0.5

export const EVENT_TYPES = [
  'Aniversário',
  'Casamento',
  'Chá de Bebê / Panela',
  'Formatura',
  'Churrasco',
  'Outro',
] as const

export interface EventEntity {
  id: string
  title: string
  eventType: string
  eventDateMillis: number
  location: string
  latitude?: number
  longitude?: number
  budget: number
  costShareMode: CostShareMode
  fixedAdultPrice: number
  fixedChildPrice: number
  invitationTemplate: string
  isActive: boolean
}

export type ParticipantType = 'ADULT' | 'CHILD'

export const PARTICIPANT_TYPE_LABEL: Record<ParticipantType, string> = {
  ADULT: 'Adulto',
  CHILD: 'Criança',
}

export interface ParticipantEntity {
  id: string
  eventId: string
  name: string
  phone: string
  type: ParticipantType
  familyGroup: string
  paidAmount: number
  notes: string
}

export type ExpenseCategory =
  | 'FOOD'
  | 'DRINK'
  | 'DECORATION'
  | 'VENUE'
  | 'ENTERTAINMENT'
  | 'OTHER'

export const EXPENSE_CATEGORY_LABEL: Record<ExpenseCategory, string> = {
  FOOD: 'Comida & Salgados',
  DRINK: 'Bebidas',
  DECORATION: 'Decoração',
  VENUE: 'Aluguel do Local',
  ENTERTAINMENT: 'Som & Animação',
  OTHER: 'Outros',
}

export const EXPENSE_CATEGORIES: ExpenseCategory[] = [
  'FOOD',
  'DRINK',
  'DECORATION',
  'VENUE',
  'ENTERTAINMENT',
  'OTHER',
]

export const EXPENSE_CATEGORY_COLOR: Record<ExpenseCategory, string> = {
  FOOD: '#FF8A80',
  DRINK: '#4FD1C5',
  DECORATION: '#FFC94D',
  VENUE: '#9B7EE8',
  ENTERTAINMENT: '#FF9AC6',
  OTHER: '#A5AEDB',
}

export interface ExpenseEntity {
  id: string
  eventId: string
  title: string
  category: ExpenseCategory
  amount: number
  isPurchased: boolean
  dateAddedMillis: number
}

export interface FinancialSummary {
  budget: number
  totalSpent: number
  totalPurchased: number
  budgetBalance: number
  totalExpectedCollection: number
  totalCollected: number
  missingCollection: number
  netBalance: number
  adultCount: number
  childCount: number
  totalParticipants: number
}
