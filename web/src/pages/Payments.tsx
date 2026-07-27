import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Printer, PartyPopper, Wallet } from 'lucide-react'
import type { PartyStore } from '../store/usePartyStore'
import { calculateParticipantTarget } from '../store/usePartyStore'
import type { ParticipantEntity } from '../types'
import { formatCurrency } from '../utils/format'

export default function Payments({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const { activeEvent, participants, financialSummary, updateParticipantPayment } = store

  const [search, setSearch] = useState('')
  const [paymentTarget, setPaymentTarget] = useState<ParticipantEntity | null>(null)

  const filtered = useMemo(
    () =>
      participants.filter(
        (p) =>
          p.name.toLowerCase().includes(search.toLowerCase()) ||
          p.familyGroup.toLowerCase().includes(search.toLowerCase()),
      ),
    [participants, search],
  )

  if (!activeEvent) {
    return (
      <>
        <div className="top-bar">
          <button className="icon-btn" onClick={() => navigate('/')}>
            <ArrowLeft size={19} strokeWidth={2.3} />
          </button>
          <h1>Módulo de Recebimentos</h1>
        </div>
        <div className="empty-state">
          <span className="emoji">
            <PartyPopper size={30} strokeWidth={2} />
          </span>
          <p>Crie um evento primeiro.</p>
        </div>
      </>
    )
  }

  const collectionPct =
    financialSummary.totalExpectedCollection > 0
      ? Math.min(1, Math.max(0, financialSummary.totalCollected / financialSummary.totalExpectedCollection))
      : 0

  return (
    <>
      <div className="top-bar">
        <button className="icon-btn" onClick={() => navigate('/')}>
          <ArrowLeft size={19} strokeWidth={2.3} />
        </button>
        <h1 style={{ flex: 1 }}>Módulo de Recebimentos</h1>
        <button className="icon-btn" title="Imprimir Relatório" onClick={() => window.print()}>
          <Printer size={19} strokeWidth={2.2} />
        </button>
      </div>

      <div className="app-content">
        <div className="page">
          <div
            style={{
              position: 'relative',
              overflow: 'hidden',
              borderRadius: 22,
              padding: 18,
              color: 'white',
              background: 'linear-gradient(155deg, #7c6ce0 0%, #6a5ad4 45%, #5a48c8 100%)',
              border: '1px solid rgba(255,255,255,0.25)',
              boxShadow: '0 16px 32px -12px rgba(90, 72, 200, 0.5), 0 1px 0 rgba(255,255,255,0.35) inset',
            }}
          >
            <div
              aria-hidden
              style={{
                position: 'absolute',
                top: -50,
                right: -50,
                width: 140,
                height: 140,
                borderRadius: '50%',
                background: 'radial-gradient(circle, rgba(255,255,255,0.22), transparent 70%)',
              }}
            />
            <p style={{ fontWeight: 800, margin: '0 0 12px', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Wallet size={18} strokeWidth={2.3} /> Arrecadação Geral do Evento
            </p>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <div>
                <p style={{ fontSize: '0.72rem', opacity: 0.85, margin: 0, fontWeight: 600 }}>Total Arrecadado</p>
                <strong style={{ fontSize: '1.2rem', color: '#6ee7a3' }}>
                  {formatCurrency(financialSummary.totalCollected)}
                </strong>
              </div>
              <div style={{ textAlign: 'right' }}>
                <p style={{ fontSize: '0.72rem', opacity: 0.85, margin: 0, fontWeight: 600 }}>Valor Faltante</p>
                <strong style={{ fontSize: '1.2rem', color: financialSummary.missingCollection > 0 ? '#ffb1b1' : '#6ee7a3' }}>
                  {formatCurrency(financialSummary.missingCollection)}
                </strong>
              </div>
            </div>
            <div className="progress-track" style={{ marginTop: 12, background: 'rgba(255,255,255,0.22)' }}>
              <div className="progress-fill" style={{ width: `${collectionPct * 100}%`, background: 'linear-gradient(90deg, #6ee7a3, #22c55e)' }} />
            </div>
            <p style={{ fontSize: '0.72rem', opacity: 0.85, marginTop: 6, fontWeight: 600 }}>
              Meta de Arrecadação: {formatCurrency(financialSummary.totalExpectedCollection)} •{' '}
              {Math.round(collectionPct * 100)}% Concluído
            </p>
          </div>

          <button className="btn btn-outline btn-block" onClick={() => window.print()}>
            <Printer size={16} strokeWidth={2.3} /> Imprimir Relatório
          </button>

          <input
            type="text"
            placeholder="Buscar participante ou família..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          {filtered.map((p) => {
            const target = calculateParticipantTarget(p, activeEvent, participants)
            const isPaidFull = p.paidAmount >= target && target > 0
            const isPartial = p.paidAmount > 0 && !isPaidFull

            return (
              <div key={p.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{ flex: 1 }}>
                  <strong style={{ fontSize: '0.88rem' }}>{p.name}</strong>
                  <p style={{ margin: '2px 0', fontSize: '0.75rem', color: 'var(--on-surface-variant)' }}>
                    {p.familyGroup} • Meta: {formatCurrency(target)}
                  </p>
                  <p
                    style={{
                      margin: 0,
                      fontSize: '0.78rem',
                      fontWeight: 700,
                      color: isPaidFull ? 'var(--success)' : isPartial ? 'var(--warning)' : 'var(--danger)',
                    }}
                  >
                    Pago até agora: {formatCurrency(p.paidAmount)}
                  </p>
                </div>
                <button
                  className="btn"
                  style={{
                    background: isPaidFull ? 'var(--secondary-container)' : 'var(--primary)',
                    color: isPaidFull ? 'var(--on-secondary-container)' : 'white',
                  }}
                  onClick={() => setPaymentTarget(p)}
                >
                  {isPaidFull ? 'Alterar' : 'Dar Baixa'}
                </button>
              </div>
            )
          })}
        </div>
      </div>

      {paymentTarget && (
        <div className="overlay" onClick={() => setPaymentTarget(null)}>
          <PaymentBody
            participant={paymentTarget}
            target={calculateParticipantTarget(paymentTarget, activeEvent, participants)}
            onClose={() => setPaymentTarget(null)}
            onConfirm={(amount) => {
              updateParticipantPayment(paymentTarget.id, amount)
              setPaymentTarget(null)
            }}
          />
        </div>
      )}
    </>
  )
}

function PaymentBody({
  participant,
  target,
  onClose,
  onConfirm,
}: {
  participant: ParticipantEntity
  target: number
  onClose: () => void
  onConfirm: (amount: number) => void
}) {
  const [amountText, setAmountText] = useState(participant.paidAmount.toString())
  return (
    <div className="dialog" onClick={(e) => e.stopPropagation()}>
      <h2>Dar Baixa em Recebimento</h2>
      <p>Participante: {participant.name}</p>
      <p>Meta Calculada: {formatCurrency(target)}</p>
      <div className="field">
        <label className="field-label">Valor Recebido / Pago (R$)</label>
        <input type="number" value={amountText} onChange={(e) => setAmountText(e.target.value)} />
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => setAmountText(target.toString())}>
          Quitar Total
        </button>
        <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => setAmountText('0')}>
          Zerar
        </button>
      </div>
      <div className="dialog-actions">
        <button className="btn btn-outline" onClick={onClose}>
          Cancelar
        </button>
        <button className="btn btn-primary" onClick={() => onConfirm(parseFloat(amountText.replace(',', '.')) || 0)}>
          Confirmar
        </button>
      </div>
    </div>
  )
}
