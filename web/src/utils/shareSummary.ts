import { calculateParticipantTarget } from '../store/usePartyStore'
import type { EventEntity, FinancialSummary, ParticipantEntity } from '../types'
import { formatCurrency, formatDate } from './format'

export interface PaymentSplit {
  paid: ParticipantEntity[]
  pending: ParticipantEntity[]
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

export function buildShareSummaryText(
  event: EventEntity,
  participants: ParticipantEntity[],
  summary: FinancialSummary,
): string {
  const { paid, pending } = splitParticipantsByPayment(event, participants)
  const lines: string[] = []

  lines.push(`🎉 ${event.title}`)
  lines.push(`📅 ${formatDate(event.eventDateMillis)}`)
  if (event.location) lines.push(`📍 ${event.location}`)
  lines.push('')

  lines.push('💰 RESUMO FINANCEIRO')
  lines.push(`Orçamento: ${formatCurrency(summary.budget)}`)
  lines.push(`Total Gasto: ${formatCurrency(summary.totalSpent)}`)
  lines.push(`Arrecadado: ${formatCurrency(summary.totalCollected)}`)
  lines.push(`Falta Arrecadar: ${formatCurrency(summary.missingCollection)}`)
  lines.push('')

  lines.push(`👥 CONVIDADOS (${participants.length})`)
  if (participants.length === 0) {
    lines.push('Nenhum convidado cadastrado ainda.')
  } else {
    for (const p of participants) {
      lines.push(`- ${p.name}${p.familyGroup ? ` (${p.familyGroup})` : ''}`)
    }
  }
  lines.push('')

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
  }

  return lines.join('\n')
}
