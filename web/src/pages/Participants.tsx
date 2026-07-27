import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  UserPlus,
  SearchX,
  MessageCircle,
  CheckCircle2,
  Pencil,
  Trash2,
  User,
  Baby,
  PartyPopper,
  Plus,
} from 'lucide-react'
import type { PartyStore } from '../store/usePartyStore'
import { calculateDefineLaterSuggestion, calculateParticipantTarget } from '../store/usePartyStore'
import type { ParticipantEntity, ParticipantType } from '../types'
import { formatCurrency } from '../utils/format'
import { openWhatsAppMessage } from '../utils/links'
import { formatDate } from '../utils/format'

export default function Participants({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const { activeEvent, participants, financialSummary, addParticipant, updateParticipant, updateParticipantPayment, deleteParticipant } = store

  const [search, setSearch] = useState('')
  const [familyFilter, setFamilyFilter] = useState('TODAS')
  const [showAdd, setShowAdd] = useState(false)
  const [editing, setEditing] = useState<ParticipantEntity | null>(null)
  const [paymentTarget, setPaymentTarget] = useState<ParticipantEntity | null>(null)

  const families = useMemo(
    () => ['TODAS', ...Array.from(new Set(participants.map((p) => p.familyGroup || 'Sem Família'))).sort()],
    [participants],
  )

  const filtered = useMemo(
    () =>
      participants.filter((p) => {
        const matchesSearch =
          p.name.toLowerCase().includes(search.toLowerCase()) || p.phone.includes(search)
        const matchesFamily = familyFilter === 'TODAS' || p.familyGroup === familyFilter
        return matchesSearch && matchesFamily
      }),
    [participants, search, familyFilter],
  )

  if (!activeEvent) {
    return (
      <>
        <div className="top-bar">
          <button className="icon-btn" onClick={() => navigate('/')}>
            <ArrowLeft size={19} strokeWidth={2.3} />
          </button>
          <h1>Convidados & Rateio</h1>
        </div>
        <div className="empty-state">
          <span className="emoji">
            <PartyPopper size={30} strokeWidth={2} />
          </span>
          <p>Crie um evento primeiro para cadastrar convidados.</p>
        </div>
      </>
    )
  }

  return (
    <>
      <div className="top-bar">
        <button className="icon-btn" onClick={() => navigate('/')}>
          <ArrowLeft size={19} strokeWidth={2.3} />
        </button>
        <h1 style={{ flex: 1 }}>Convidados & Rateio</h1>
        <button className="icon-btn" onClick={() => setShowAdd(true)}>
          <UserPlus size={19} strokeWidth={2.2} />
        </button>
      </div>

      <div className="app-content">
        <div className="page">
          <div className="card" style={{ background: 'linear-gradient(155deg, rgba(255,255,255,0.75), rgba(233,227,255,0.55))' }}>
            <div style={{ display: 'flex', justifyContent: 'space-around', textAlign: 'center' }}>
              <div>
                <p style={{ fontSize: '0.7rem', margin: 0, fontWeight: 600, color: 'var(--on-surface-variant)' }}>Total</p>
                <strong>{participants.length} pessoas</strong>
              </div>
              <div>
                <p style={{ fontSize: '0.7rem', margin: 0, fontWeight: 600, color: 'var(--on-surface-variant)' }}>Adultos</p>
                <strong style={{ color: 'var(--primary)' }}>{financialSummary.adultCount}</strong>
              </div>
              <div>
                <p style={{ fontSize: '0.7rem', margin: 0, fontWeight: 600, color: 'var(--on-surface-variant)' }}>Crianças</p>
                <strong style={{ color: '#e08a1e' }}>{financialSummary.childCount}</strong>
              </div>
            </div>
            {activeEvent.costShareMode === 'DEFINE_LATER' && participants.length > 0 && (
              <p
                style={{
                  fontSize: '0.75rem',
                  textAlign: 'center',
                  margin: '10px 0 0',
                  fontWeight: 600,
                  color: 'var(--on-surface-variant)',
                }}
              >
                Valor calculado pela lista: Adulto{' '}
                <strong style={{ color: 'var(--on-surface)' }}>
                  {formatCurrency(calculateDefineLaterSuggestion(activeEvent, participants).adultPrice)}
                </strong>{' '}
                • Criança{' '}
                <strong style={{ color: 'var(--on-surface)' }}>
                  {formatCurrency(calculateDefineLaterSuggestion(activeEvent, participants).childPrice)}
                </strong>
              </p>
            )}
          </div>

          <input
            type="text"
            placeholder="Buscar por nome ou telefone..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          {families.length > 2 && (
            <div className="chip-row">
              {families.map((f) => (
                <button
                  key={f}
                  className={`chip ${familyFilter === f ? 'selected' : ''}`}
                  onClick={() => setFamilyFilter(f)}
                >
                  {f}
                </button>
              ))}
            </div>
          )}

          {filtered.length === 0 ? (
            <div className="empty-state">
              <span className="emoji">
                <SearchX size={28} strokeWidth={2} />
              </span>
              <p>{search ? 'Nenhum convidado encontrado.' : 'Nenhum convidado cadastrado ainda.'}</p>
            </div>
          ) : (
            filtered.map((p) => {
              const target = calculateParticipantTarget(p, activeEvent, participants)
              const isPaidFull = p.paidAmount >= target && target > 0
              const isPartial = p.paidAmount > 0 && !isPaidFull

              return (
                <div key={p.id} className="card" style={{ padding: 14 }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
                    <div
                      style={{
                        width: 42,
                        height: 42,
                        borderRadius: 14,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        background:
                          p.type === 'ADULT'
                            ? 'linear-gradient(155deg, #ffffff, #e9e2ff)'
                            : 'linear-gradient(155deg, #ffffff, #ffedd1)',
                        color: p.type === 'ADULT' ? 'var(--primary)' : '#c9820f',
                        flexShrink: 0,
                        boxShadow: '0 3px 8px -2px rgba(90, 60, 160, 0.2), 0 1px 0 rgba(255,255,255,0.6) inset',
                      }}
                    >
                      {p.type === 'ADULT' ? <User size={20} strokeWidth={2.2} /> : <Baby size={20} strokeWidth={2.2} />}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                        <strong style={{ fontSize: '0.9rem' }}>{p.name}</strong>
                        <span className="badge" style={{ background: 'var(--surface-variant)' }}>
                          {p.familyGroup}
                        </span>
                      </div>
                      <p style={{ margin: '2px 0', fontSize: '0.75rem', color: 'var(--on-surface-variant)' }}>
                        {p.type === 'ADULT' ? 'Adulto' : 'Criança'} • Rateio: {formatCurrency(target)}
                      </p>
                      <span
                        className={`badge ${isPaidFull ? 'badge-success' : isPartial ? 'badge-warning' : 'badge-danger'}`}
                      >
                        {isPaidFull
                          ? `PAGO: ${formatCurrency(p.paidAmount)}`
                          : isPartial
                            ? `PARCIAL: ${formatCurrency(p.paidAmount)} / ${formatCurrency(target)}`
                            : 'PENDENTE: R$ 0,00'}
                      </span>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 4, marginTop: 10, justifyContent: 'flex-end' }}>
                    <button
                      className="icon-btn"
                      title="Enviar WhatsApp"
                      onClick={() => {
                        const message = (activeEvent.invitationTemplate || '')
                          .replace('{nome}', p.name)
                          .replace('{evento}', activeEvent.title)
                          .replace('{data}', formatDate(activeEvent.eventDateMillis))
                          .replace('{local}', activeEvent.location)
                          .replace('{valor}', formatCurrency(target))
                        openWhatsAppMessage(p.phone, message)
                      }}
                      style={{ color: 'var(--whatsapp)' }}
                    >
                      <MessageCircle size={17} strokeWidth={2.2} />
                    </button>
                    <button className="icon-btn" title="Dar baixa" style={{ color: 'var(--success)' }} onClick={() => setPaymentTarget(p)}>
                      <CheckCircle2 size={17} strokeWidth={2.2} />
                    </button>
                    <button className="icon-btn" title="Editar" onClick={() => setEditing(p)}>
                      <Pencil size={16} strokeWidth={2.2} />
                    </button>
                    <button
                      className="icon-btn"
                      title="Excluir"
                      style={{ color: 'var(--danger)' }}
                      onClick={() => deleteParticipant(p)}
                    >
                      <Trash2 size={16} strokeWidth={2.2} />
                    </button>
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>

      <button className="fab" onClick={() => setShowAdd(true)}>
        <Plus size={24} strokeWidth={2.6} />
      </button>

      {(showAdd || editing) && (
        <ParticipantDialog
          participant={editing}
          onClose={() => {
            setShowAdd(false)
            setEditing(null)
          }}
          onSave={(data) => {
            if (editing) {
              updateParticipant({ ...editing, ...data })
            } else {
              addParticipant({ ...data, eventId: activeEvent.id, paidAmount: 0 })
            }
            setShowAdd(false)
            setEditing(null)
          }}
        />
      )}

      {paymentTarget && (
        <PaymentDialog
          participant={paymentTarget}
          target={calculateParticipantTarget(paymentTarget, activeEvent, participants)}
          onClose={() => setPaymentTarget(null)}
          onConfirm={(amount) => {
            updateParticipantPayment(paymentTarget.id, amount)
            setPaymentTarget(null)
          }}
        />
      )}
    </>
  )
}

function ParticipantDialog({
  participant,
  onClose,
  onSave,
}: {
  participant: ParticipantEntity | null
  onClose: () => void
  onSave: (data: { name: string; phone: string; familyGroup: string; type: ParticipantType; notes: string }) => void
}) {
  const [name, setName] = useState(participant?.name ?? '')
  const [phone, setPhone] = useState(participant?.phone ?? '')
  const [familyGroup, setFamilyGroup] = useState(participant?.familyGroup ?? 'Família Silva')
  const [type, setType] = useState<ParticipantType>(participant?.type ?? 'ADULT')

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2>{participant ? 'Editar Convidado' : 'Novo Convidado'}</h2>
        <div className="field">
          <label className="field-label">Nome do Participante</label>
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label className="field-label">Telefone / WhatsApp (DDD+Número)</label>
          <input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="Ex: 11999998888"
          />
        </div>
        <div className="field">
          <label className="field-label">Grupo / Nome da Família</label>
          <input
            type="text"
            value={familyGroup}
            onChange={(e) => setFamilyGroup(e.target.value)}
            placeholder="Ex: Família Silva, Amigos do Trabalho"
          />
        </div>
        <label className="field-label">Classificação</label>
        <div className="chip-row">
          <button className={`chip ${type === 'ADULT' ? 'selected' : ''}`} onClick={() => setType('ADULT')}>
            Adulto
          </button>
          <button className={`chip ${type === 'CHILD' ? 'selected' : ''}`} onClick={() => setType('CHILD')}>
            Criança
          </button>
        </div>
        <div className="dialog-actions">
          <button className="btn btn-outline" onClick={onClose}>
            Cancelar
          </button>
          <button
            className="btn btn-primary"
            onClick={() => {
              if (name.trim()) {
                onSave({
                  name: name.trim(),
                  phone,
                  familyGroup: familyGroup.trim() || 'Sem Família',
                  type,
                  notes: participant?.notes ?? '',
                })
              }
            }}
          >
            Salvar
          </button>
        </div>
      </div>
    </div>
  )
}

function PaymentDialog({
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
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2>Dar Baixa no Pagamento</h2>
        <p>Participante: {participant.name}</p>
        <p>Meta Individual: {formatCurrency(target)}</p>
        <div className="field">
          <label className="field-label">Valor Pago (R$)</label>
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
          <button
            className="btn btn-primary"
            onClick={() => onConfirm(parseFloat(amountText.replace(',', '.')) || 0)}
          >
            Confirmar Baixa
          </button>
        </div>
      </div>
    </div>
  )
}
