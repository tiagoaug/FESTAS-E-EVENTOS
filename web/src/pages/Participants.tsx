import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  UserPlus,
  SearchX,
  MessageCircle,
  CheckCircle2,
  Circle,
  Pencil,
  Trash2,
  User,
  Baby,
  PartyPopper,
  Plus,
  Calculator,
} from 'lucide-react'
import type { PartyStore } from '../store/usePartyStore'
import { calculateDefineLaterSuggestion, calculateParticipantTarget } from '../store/usePartyStore'
import type { ParticipantEntity, ParticipantType } from '../types'
import { formatCurrency } from '../utils/format'
import { openWhatsAppMessage } from '../utils/links'
import { formatDate } from '../utils/format'
import MiniCalculator from '../components/MiniCalculator'
import { useSettings } from '../context/settingsContextValue'

export default function Participants({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const { useWhatsApp } = useSettings()
  const {
    activeEvent,
    participants,
    financialSummary,
    addParticipant,
    updateParticipant,
    updateParticipantPayment,
    toggleParticipantConfirmed,
    deleteParticipant,
  } = store

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

          {families.length > 1 && (
            <div className="chip-row" style={{ flexWrap: 'wrap' }}>
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
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        <span
                          className={`badge ${isPaidFull ? 'badge-success' : isPartial ? 'badge-warning' : 'badge-danger'}`}
                        >
                          {isPaidFull
                            ? `PAGO: ${formatCurrency(p.paidAmount)}`
                            : isPartial
                              ? `PARCIAL: ${formatCurrency(p.paidAmount)} / ${formatCurrency(target)}`
                              : 'PENDENTE: R$ 0,00'}
                        </span>
                        <button
                          type="button"
                          className={`badge ${p.confirmed ? 'badge-success' : 'badge-warning'}`}
                          style={{ border: 'none', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4 }}
                          title="Clique para alternar confirmação de presença"
                          onClick={() => toggleParticipantConfirmed(p.id, !p.confirmed)}
                        >
                          {p.confirmed ? (
                            <CheckCircle2 size={12} strokeWidth={2.4} />
                          ) : (
                            <Circle size={12} strokeWidth={2.4} />
                          )}
                          {p.confirmed ? 'PRESENÇA CONFIRMADA' : 'AGUARDANDO CONFIRMAÇÃO'}
                        </button>
                      </div>
                    </div>
                  </div>
                  <div className="action-capsule">
                    {useWhatsApp && (
                      <button
                        type="button"
                        className="action-pill-btn"
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
                      >
                        <MessageCircle size={18} strokeWidth={2.2} color="var(--whatsapp)" />
                        <span>WhatsApp</span>
                      </button>
                    )}
                    <button
                      type="button"
                      className="action-pill-btn"
                      title="Dar baixa no pagamento"
                      onClick={() => setPaymentTarget(p)}
                    >
                      <CheckCircle2 size={18} strokeWidth={2.2} color="var(--success)" />
                      <span>Receber</span>
                    </button>
                    <button
                      type="button"
                      className="action-pill-btn"
                      title="Editar convidado"
                      onClick={() => setEditing(p)}
                    >
                      <Pencil size={17} strokeWidth={2.2} color="var(--primary)" />
                      <span>Editar</span>
                    </button>
                    <button
                      type="button"
                      className="action-pill-btn"
                      title="Excluir convidado"
                      onClick={() => deleteParticipant(p)}
                    >
                      <Trash2 size={17} strokeWidth={2.2} color="var(--danger)" />
                      <span>Excluir</span>
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

      {showAdd && (
        <AddParticipantDialog
          onClose={() => setShowAdd(false)}
          onSaveSingle={(data) => {
            addParticipant({ ...data, eventId: activeEvent.id, paidAmount: 0, confirmed: false })
            setShowAdd(false)
          }}
          onSaveFamily={(familyName, members) => {
            members.forEach((member) => {
              addParticipant({
                name: member.name,
                phone: member.phone,
                type: member.type,
                familyGroup: familyName,
                notes: '',
                confirmed: false,
                eventId: activeEvent.id,
                paidAmount: 0,
              })
            })
            setShowAdd(false)
          }}
        />
      )}

      {editing && (
        <EditParticipantDialog
          participant={editing}
          onClose={() => setEditing(null)}
          onSave={(data) => {
            updateParticipant({ ...editing, ...data })
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

function EditParticipantDialog({
  participant,
  onClose,
  onSave,
}: {
  participant: ParticipantEntity
  onClose: () => void
  onSave: (data: { name: string; phone: string; familyGroup: string; type: ParticipantType; notes: string }) => void
}) {
  const [name, setName] = useState(participant.name)
  const [phone, setPhone] = useState(participant.phone)
  const [familyGroup, setFamilyGroup] = useState(participant.familyGroup)
  const [type, setType] = useState<ParticipantType>(participant.type)

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2>Editar Convidado</h2>
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
                  notes: participant.notes,
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

type AddMode = 'AVULSO' | 'FAMILIA'

interface FamilyMemberDraft {
  name: string
  phone: string
  type: ParticipantType
}

function emptyMember(): FamilyMemberDraft {
  return { name: '', phone: '', type: 'ADULT' }
}

function AddParticipantDialog({
  onClose,
  onSaveSingle,
  onSaveFamily,
}: {
  onClose: () => void
  onSaveSingle: (data: { name: string; phone: string; familyGroup: string; type: ParticipantType; notes: string }) => void
  onSaveFamily: (familyName: string, members: FamilyMemberDraft[]) => void
}) {
  const [mode, setMode] = useState<AddMode>('AVULSO')

  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [type, setType] = useState<ParticipantType>('ADULT')

  const [familyName, setFamilyName] = useState('')
  const [members, setMembers] = useState<FamilyMemberDraft[]>([emptyMember()])

  const updateMember = (index: number, patch: Partial<FamilyMemberDraft>) => {
    setMembers((prev) => prev.map((m, i) => (i === index ? { ...m, ...patch } : m)))
  }
  const addMemberRow = () => setMembers((prev) => [...prev, emptyMember()])
  const removeMemberRow = (index: number) => setMembers((prev) => prev.filter((_, i) => i !== index))

  const validMemberCount = members.filter((m) => m.name.trim()).length

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2>Novo Convidado</h2>

        <div className="chip-row" style={{ marginBottom: 14 }}>
          <button
            className={`chip ${mode === 'AVULSO' ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => setMode('AVULSO')}
          >
            Convidado Avulso
          </button>
          <button
            className={`chip ${mode === 'FAMILIA' ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => setMode('FAMILIA')}
          >
            Família
          </button>
        </div>

        {mode === 'AVULSO' ? (
          <>
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
                    onSaveSingle({ name: name.trim(), phone, familyGroup: 'Sem Família', type, notes: '' })
                  }
                }}
              >
                Salvar
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="field">
              <label className="field-label">Nome da Família</label>
              <input
                type="text"
                value={familyName}
                onChange={(e) => setFamilyName(e.target.value)}
                placeholder="Ex: Família Silva"
              />
            </div>

            <label className="field-label">Integrantes</label>
            {members.map((member, index) => (
              <div key={index} style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 8 }}>
                <input
                  type="text"
                  value={member.name}
                  onChange={(e) => updateMember(index, { name: e.target.value })}
                  placeholder={`Nome ${index + 1}`}
                  style={{ flex: 2 }}
                />
                <input
                  type="tel"
                  value={member.phone}
                  onChange={(e) => updateMember(index, { phone: e.target.value })}
                  placeholder="Telefone"
                  style={{ flex: 1 }}
                />
                <button
                  type="button"
                  className={`chip ${member.type === 'ADULT' ? 'selected' : ''}`}
                  title={member.type === 'ADULT' ? 'Adulto (toque para marcar Criança)' : 'Criança (toque para marcar Adulto)'}
                  onClick={() => updateMember(index, { type: member.type === 'ADULT' ? 'CHILD' : 'ADULT' })}
                  style={{ flexShrink: 0, fontSize: '0.7rem', padding: '8px 10px' }}
                >
                  {member.type === 'ADULT' ? 'Ad.' : 'Cri.'}
                </button>
                {members.length > 1 && (
                  <button
                    type="button"
                    className="icon-btn"
                    style={{ color: 'var(--danger)', flexShrink: 0 }}
                    onClick={() => removeMemberRow(index)}
                  >
                    <Trash2 size={15} strokeWidth={2.2} />
                  </button>
                )}
              </div>
            ))}
            <button className="btn btn-outline btn-block" onClick={addMemberRow} style={{ marginBottom: 14 }}>
              <Plus size={15} strokeWidth={2.3} /> Adicionar Integrante
            </button>

            <div className="dialog-actions">
              <button className="btn btn-outline" onClick={onClose}>
                Cancelar
              </button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  const validMembers = members
                    .filter((m) => m.name.trim())
                    .map((m) => ({ ...m, name: m.name.trim() }))
                  if (familyName.trim() && validMembers.length > 0) {
                    onSaveFamily(familyName.trim(), validMembers)
                  }
                }}
              >
                Salvar Família ({validMemberCount})
              </button>
            </div>
          </>
        )}
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
  const [partialText, setPartialText] = useState('')
  const [calcTarget, setCalcTarget] = useState<'total' | 'partial' | null>(null)

  const addPartial = () => {
    const partial = parseFloat(partialText.replace(',', '.')) || 0
    if (partial <= 0) return
    const current = parseFloat(amountText.replace(',', '.')) || 0
    setAmountText((current + partial).toString())
    setPartialText('')
  }

  return (
    <>
      <div className="overlay" onClick={onClose}>
        <div className="dialog" onClick={(e) => e.stopPropagation()}>
          <h2>Dar Baixa no Pagamento</h2>
          <p>Participante: {participant.name}</p>
          <p>Meta Individual: {formatCurrency(target)}</p>

          <div className="field">
            <label className="field-label">Valor Total Recebido (R$)</label>
            <div style={{ display: 'flex', gap: 8 }}>
              <input
                type="number"
                value={amountText}
                onChange={(e) => setAmountText(e.target.value)}
                style={{ flex: 1 }}
              />
              <button
                type="button"
                className="icon-btn"
                title="Abrir calculadora"
                onClick={() => setCalcTarget('total')}
              >
                <Calculator size={18} strokeWidth={2.2} />
              </button>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => setAmountText(target.toString())}>
              Quitar Total
            </button>
            <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => setAmountText('0')}>
              Zerar
            </button>
          </div>

          <div className="field">
            <label className="field-label">Receber Parcial Agora (R$)</label>
            <div style={{ display: 'flex', gap: 8 }}>
              <input
                type="number"
                value={partialText}
                onChange={(e) => setPartialText(e.target.value)}
                placeholder="Ex: 50,00"
                style={{ flex: 1 }}
              />
              <button
                type="button"
                className="icon-btn"
                title="Abrir calculadora"
                onClick={() => setCalcTarget('partial')}
              >
                <Calculator size={18} strokeWidth={2.2} />
              </button>
              <button type="button" className="btn btn-outline" onClick={addPartial}>
                Adicionar
              </button>
            </div>
            <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: '4px 0 0' }}>
              Soma ao valor total recebido acima, sem substituir o que já foi pago.
            </p>
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

      {calcTarget && (
        <MiniCalculator
          onClose={() => setCalcTarget(null)}
          onUse={(value) => {
            if (calcTarget === 'total') setAmountText(value.toString())
            else setPartialText(value.toString())
          }}
        />
      )}
    </>
  )
}
