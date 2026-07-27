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
  /** Confirmação de presença no evento. */
  confirmed: boolean
}

export interface CategoryEntity {
  id: string
  name: string
  color: string
}

/** Seeded once per user the first time their category list is empty. */
export const DEFAULT_CATEGORIES: Omit<CategoryEntity, 'id'>[] = [
  { name: 'Comida & Salgados', color: '#FF8A80' },
  { name: 'Bebidas', color: '#4FD1C5' },
  { name: 'Decoração', color: '#FFC94D' },
  { name: 'Aluguel do Local', color: '#9B7EE8' },
  { name: 'Som & Animação', color: '#FF9AC6' },
  { name: 'Outros', color: '#A5AEDB' },
]

/** Curated swatches offered when creating/editing a category. */
export const CATEGORY_COLOR_SWATCHES = [
  '#FF8A80',
  '#4FD1C5',
  '#FFC94D',
  '#9B7EE8',
  '#FF9AC6',
  '#A5AEDB',
  '#60A5FA',
  '#34D399',
  '#FBBF24',
  '#F472B6',
  '#A78BFA',
  '#F87171',
]

export const FALLBACK_CATEGORY_COLOR = '#A5AEDB'
export const FALLBACK_CATEGORY_LABEL = 'Sem categoria'

export interface ExpenseEntity {
  id: string
  eventId: string
  title: string
  category: string
  amount: number
  /** Status de compra: o item já foi comprado? */
  isPurchased: boolean
  /** Status de pagamento: o item já foi pago ao fornecedor? Independente de isPurchased. */
  isPaid: boolean
  dateAddedMillis: number
  notes: string
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
