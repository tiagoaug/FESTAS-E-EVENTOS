import { useState } from 'react'
import { Check, Copy, ImageDown, Users, X } from 'lucide-react'
import { calculateParticipantTarget } from '../store/usePartyStore'
import type { EventEntity, FinancialSummary, ParticipantEntity } from '../types'
import { formatCurrency } from '../utils/format'
import { buildShareSummaryText, splitParticipantsByPayment } from '../utils/shareSummary'

interface ShareSummaryDialogProps {
  event: EventEntity
  participants: ParticipantEntity[]
  summary: FinancialSummary
  onExportJpg: () => void
  onClose: () => void
}

function ListSection({ title, count, children }: { title: string; count: number; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 14 }}>
      <p style={{ fontWeight: 700, fontSize: '0.82rem', margin: '0 0 6px' }}>
        {title} ({count})
      </p>
      <div
        style={{
          maxHeight: 140,
          overflowY: 'auto',
          background: 'var(--surface-variant)',
          borderRadius: 12,
          padding: count > 0 ? '8px 12px' : '12px',
        }}
      >
        {children}
      </div>
    </div>
  )
}

export default function ShareSummaryDialog({
  event,
  participants,
  summary,
  onExportJpg,
  onClose,
}: ShareSummaryDialogProps) {
  const [copied, setCopied] = useState(false)
  const { paid, pending } = splitParticipantsByPayment(event, participants)

  const handleCopy = async () => {
    const text = buildShareSummaryText(event, participants, summary)
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      alert('Não foi possível copiar automaticamente. Selecione e copie o texto manualmente.')
    }
  }

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Users size={18} strokeWidth={2.4} color="var(--primary)" />
          Painel de Compartilhamento
        </h2>

        <div className="grid-2" style={{ marginBottom: 14 }}>
          <div className="metric-tile">
            <div>
              <p className="metric-title">Orçamento</p>
              <p className="metric-value">{formatCurrency(summary.budget)}</p>
            </div>
          </div>
          <div className="metric-tile">
            <div>
              <p className="metric-title">Total Gasto</p>
              <p className="metric-value">{formatCurrency(summary.totalSpent)}</p>
            </div>
          </div>
        </div>

        <ListSection title="Convidados" count={participants.length}>
          {participants.length === 0 ? (
            <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
              Nenhum convidado cadastrado ainda.
            </span>
          ) : (
            participants.map((p) => (
              <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0' }}>
                {p.name}
                {p.familyGroup && (
                  <span style={{ color: 'var(--on-surface-variant)' }}> • {p.familyGroup}</span>
                )}
              </p>
            ))
          )}
        </ListSection>

        {event.costShareMode !== 'ORGANIZER_ONLY' && (
          <>
            <ListSection title="Já Pagaram" count={paid.length}>
              {paid.length === 0 ? (
                <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                  Ninguém quitou o valor ainda.
                </span>
              ) : (
                paid.map((p) => (
                  <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0', color: 'var(--success)' }}>
                    {p.name}: {formatCurrency(p.paidAmount)}
                  </p>
                ))
              )}
            </ListSection>

            <ListSection title="Faltam Pagar" count={pending.length}>
              {pending.length === 0 ? (
                <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                  Todo mundo está em dia!
                </span>
              ) : (
                pending.map((p) => {
                  const target = calculateParticipantTarget(p, event, participants)
                  const missing = Math.max(0, target - p.paidAmount)
                  return (
                    <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0', color: 'var(--danger)' }}>
                      {p.name}: falta {formatCurrency(missing)}
                    </p>
                  )
                })
              )}
            </ListSection>
          </>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 4 }}>
          <button className="btn btn-outline btn-block" onClick={handleCopy}>
            {copied ? <Check size={16} strokeWidth={2.4} /> : <Copy size={16} strokeWidth={2.3} />}
            {copied ? 'Copiado!' : 'Copiar como Texto'}
          </button>
          <button
            className="btn btn-primary btn-block"
            onClick={() => {
              onClose()
              onExportJpg()
            }}
          >
            <ImageDown size={16} strokeWidth={2.3} /> Exportar JPG
          </button>
        </div>

        <div className="dialog-actions">
          <button className="btn btn-outline btn-block" onClick={onClose}>
            <X size={16} strokeWidth={2.3} /> Fechar
          </button>
        </div>
      </div>
    </div>
  )
}
