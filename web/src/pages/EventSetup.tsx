import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowLeft, Calculator, Save, CalendarPlus, Trash2 } from 'lucide-react'
import type { PartyStore } from '../store/usePartyStore'
import { EVENT_TYPES, type CostShareMode, type EventEntity, DEFINE_LATER_CHILD_WEIGHT } from '../types'
import { formatCurrency, formatDateOnly } from '../utils/format'
import { googleCalendarUrl } from '../utils/links'
import EventLocationCard from '../components/EventLocationCard'
import MiniCalculator from '../components/MiniCalculator'

function toDateInputValue(millis: number) {
  const d = new Date(millis)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function toTimeInputValue(millis: number) {
  const d = new Date(millis)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export default function EventSetup({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { activeEvent, events, participants, addEvent, updateEvent, selectActiveEvent, deleteEvent } = store

  const isCreatingNew = searchParams.get('new') === '1'
  const editingEvent = isCreatingNew ? null : activeEvent

  const defaultDate = new Date(Date.now() + 7 * 24 * 3600 * 1000)

  const [title, setTitle] = useState(editingEvent?.title ?? '')
  const [eventType, setEventType] = useState(editingEvent?.eventType ?? 'Aniversário')
  const [location, setLocation] = useState(editingEvent?.location ?? '')
  const [budgetText, setBudgetText] = useState(editingEvent?.budget?.toString() ?? '')
  const [dateStr, setDateStr] = useState(
    editingEvent ? toDateInputValue(editingEvent.eventDateMillis) : toDateInputValue(defaultDate.getTime()),
  )
  const [timeStr, setTimeStr] = useState(
    editingEvent ? toTimeInputValue(editingEvent.eventDateMillis) : '19:00',
  )
  const [costShareMode, setCostShareMode] = useState<CostShareMode>(
    editingEvent?.costShareMode ?? 'EQUAL',
  )
  const [fixedAdultText, setFixedAdultText] = useState(
    editingEvent?.fixedAdultPrice?.toString() ?? '50.0',
  )
  const [fixedChildText, setFixedChildText] = useState(
    editingEvent?.fixedChildPrice?.toString() ?? '20.0',
  )
  const [deleteTarget, setDeleteTarget] = useState<EventEntity | null>(null)
  const [showBudgetCalculator, setShowBudgetCalculator] = useState(false)
  const [latitude, setLatitude] = useState<number | undefined>(editingEvent?.latitude)
  const [longitude, setLongitude] = useState<number | undefined>(editingEvent?.longitude)

  const relevantParticipants = editingEvent ? participants : []
  const previewAdultCount = relevantParticipants.filter((p) => p.type === 'ADULT').length
  const previewChildCount = relevantParticipants.filter((p) => p.type === 'CHILD').length
  const previewTotalWeight = previewAdultCount + previewChildCount * DEFINE_LATER_CHILD_WEIGHT
  const previewBudget = parseFloat(budgetText.replace(',', '.')) || 0
  const previewAdultPrice = previewTotalWeight > 0 ? previewBudget / previewTotalWeight : 0
  const previewChildPrice = previewAdultPrice * DEFINE_LATER_CHILD_WEIGHT

  const eventDateMillis = (() => {
    const [y, m, d] = dateStr.split('-').map(Number)
    const [h, min] = timeStr.split(':').map(Number)
    return new Date(y, (m || 1) - 1, d || 1, h || 0, min || 0).getTime()
  })()

  const handleSave = () => {
    const parsedBudget = parseFloat(budgetText.replace(',', '.')) || 0
    const parsedAdult = parseFloat(fixedAdultText.replace(',', '.')) || 0
    const parsedChild = parseFloat(fixedChildText.replace(',', '.')) || 0

    const eventData = {
      title: title.trim() || 'Minha Festa',
      eventType,
      location,
      latitude,
      longitude,
      budget: parsedBudget,
      eventDateMillis,
      costShareMode,
      fixedAdultPrice: parsedAdult,
      fixedChildPrice: parsedChild,
      invitationTemplate:
        editingEvent?.invitationTemplate ??
        'Olá {nome}! Você está convidado(a) para {evento} no dia {data} no local {local}. Sua contribuição: {valor}. Confirme sua presença!',
      isActive: true,
    }

    if (editingEvent) {
      updateEvent({ ...eventData, id: editingEvent.id })
    } else {
      addEvent(eventData)
    }
    navigate('/')
  }

  return (
    <>
      <div className="top-bar">
        <button className="icon-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={19} strokeWidth={2.3} />
        </button>
        <h1>Cadastro & Rateio do Evento</h1>
      </div>

      <div className="app-content">
        <div className="page">
          <div className="card">
            <p className="card-title">{editingEvent ? 'Editar Dados do Evento' : 'Novo Evento'}</p>

            <div className="field">
              <label className="field-label">Nome da Festa / Evento</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ex: Aniversário de Lucas, Churrasco de Fim de Ano"
              />
            </div>

            <label className="field-label">Tipo de Evento</label>
            <div className="chip-row" style={{ marginBottom: 12 }}>
              {EVENT_TYPES.map((type) => (
                <button
                  key={type}
                  className={`chip ${eventType === type ? 'selected' : ''}`}
                  onClick={() => setEventType(type)}
                >
                  {type}
                </button>
              ))}
            </div>

            <div className="field">
              <label className="field-label">Orçamento Total (R$)</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  type="number"
                  value={budgetText}
                  onChange={(e) => setBudgetText(e.target.value)}
                  placeholder="Ex: 2500.00"
                  style={{ flex: 1 }}
                />
                <button
                  type="button"
                  className="icon-btn"
                  title="Abrir calculadora"
                  onClick={() => setShowBudgetCalculator(true)}
                >
                  <Calculator size={18} strokeWidth={2.2} />
                </button>
              </div>
            </div>

            {showBudgetCalculator && (
              <MiniCalculator
                onClose={() => setShowBudgetCalculator(false)}
                onUse={(value) => setBudgetText(value.toString())}
              />
            )}

            <label className="field-label">Data e Hora</label>
            <div style={{ display: 'flex', gap: 10, marginBottom: 12 }}>
              <input type="date" value={dateStr} onChange={(e) => setDateStr(e.target.value)} />
              <input type="time" value={timeStr} onChange={(e) => setTimeStr(e.target.value)} />
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid var(--outline-variant)', margin: '4px 0 12px' }} />

            <p className="card-title" style={{ fontSize: '0.9rem' }}>
              Sistema de Rateio de Custos
            </p>

            {[
              {
                mode: 'EQUAL' as CostShareMode,
                title: 'Divisão Igualitária entre todos',
                desc: 'Divide o orçamento total igualmente por todos os participantes cadastrados.',
              },
              {
                mode: 'FIXED_TYPE' as CostShareMode,
                title: 'Valores Fixos (Adulto vs Criança)',
                desc: 'Define um valor fixo em R$ para adultos e outro valor em R$ para crianças.',
              },
              {
                mode: 'ORGANIZER_ONLY' as CostShareMode,
                title: 'Organizador Assume Custo Total',
                desc: 'Convidados não pagam nada (R$ 0,00). O custo é 100% do organizador.',
              },
              {
                mode: 'DEFINE_LATER' as CostShareMode,
                title: 'Definir Depois (calcular pela lista)',
                desc: 'Não define valores agora. Assim que os convidados forem cadastrados, o valor de adulto e criança é calculado automaticamente com base no orçamento e na quantidade de cada um (criança paga metade do valor do adulto).',
              },
            ].map((opt) => (
              <label
                key={opt.mode}
                style={{ display: 'flex', gap: 8, padding: '8px 0', cursor: 'pointer' }}
              >
                <input
                  type="radio"
                  checked={costShareMode === opt.mode}
                  onChange={() => setCostShareMode(opt.mode)}
                  style={{ width: 'auto', marginTop: 3 }}
                />
                <span>
                  <strong style={{ display: 'block', fontSize: '0.85rem' }}>{opt.title}</strong>
                  <span style={{ fontSize: '0.75rem', color: 'var(--on-surface-variant)' }}>
                    {opt.desc}
                  </span>
                </span>
              </label>
            ))}

            {costShareMode === 'FIXED_TYPE' && (
              <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
                <div className="field" style={{ flex: 1, marginBottom: 0 }}>
                  <label className="field-label">Valor Adulto (R$)</label>
                  <input
                    type="number"
                    value={fixedAdultText}
                    onChange={(e) => setFixedAdultText(e.target.value)}
                  />
                </div>
                <div className="field" style={{ flex: 1, marginBottom: 0 }}>
                  <label className="field-label">Valor Criança (R$)</label>
                  <input
                    type="number"
                    value={fixedChildText}
                    onChange={(e) => setFixedChildText(e.target.value)}
                  />
                </div>
              </div>
            )}

            {costShareMode === 'DEFINE_LATER' && (
              <div
                style={{
                  marginTop: 10,
                  padding: 12,
                  borderRadius: 14,
                  background: 'var(--secondary-container)',
                }}
              >
                {relevantParticipants.length === 0 ? (
                  <p style={{ fontSize: '0.78rem', margin: 0, color: 'var(--on-secondary-container)' }}>
                    Assim que você cadastrar os convidados, o valor por adulto e criança aparece aqui e nas telas
                    de Convidados, Receber e Convites automaticamente.
                  </p>
                ) : (
                  <>
                    <p style={{ fontSize: '0.78rem', fontWeight: 700, margin: '0 0 4px', color: 'var(--on-secondary-container)' }}>
                      Com base na lista atual ({previewAdultCount} adulto{previewAdultCount !== 1 ? 's' : ''}, {previewChildCount}{' '}
                      criança{previewChildCount !== 1 ? 's' : ''}):
                    </p>
                    <p style={{ fontSize: '0.82rem', margin: 0, color: 'var(--on-secondary-container)' }}>
                      Adulto: <strong>{formatCurrency(previewAdultPrice)}</strong> • Criança:{' '}
                      <strong>{formatCurrency(previewChildPrice)}</strong>
                    </p>
                  </>
                )}
              </div>
            )}

            <button className="btn btn-primary btn-block" style={{ marginTop: 16 }} onClick={handleSave}>
              <Save size={16} strokeWidth={2.3} /> {editingEvent ? 'Salvar Alterações' : 'Criar Evento'}
            </button>

            {editingEvent && (
              <a
                href={googleCalendarUrl(editingEvent.title, editingEvent.location, eventDateMillis)}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-outline btn-block"
                style={{ marginTop: 10, textDecoration: 'none' }}
              >
                <CalendarPlus size={16} strokeWidth={2.3} /> Sincronizar com Google Agenda
              </a>
            )}
          </div>

          <EventLocationCard
            location={location}
            onLocationChange={setLocation}
            latitude={latitude}
            longitude={longitude}
            onCoordsChange={(lat, lng) => {
              setLatitude(lat)
              setLongitude(lng)
            }}
          />

          {events.length > 0 && (
            <div className="card">
              <p className="card-title">Todos os Eventos Cadastrados</p>
              {events.map((ev) => (
                <div key={ev.id}>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '8px 0',
                    }}
                  >
                    <div>
                      <p
                        style={{
                          margin: 0,
                          fontWeight: ev.id === activeEvent?.id ? 700 : 400,
                          color: ev.id === activeEvent?.id ? 'var(--primary)' : 'var(--on-surface)',
                        }}
                      >
                        {ev.title}
                      </p>
                      <p style={{ margin: 0, fontSize: '0.75rem', color: 'var(--on-surface-variant)' }}>
                        {formatDateOnly(ev.eventDateMillis)} • {formatCurrency(ev.budget)}
                      </p>
                    </div>
                    <div style={{ display: 'flex', gap: 6 }}>
                      {ev.id !== activeEvent?.id && (
                        <button className="btn-danger-text" style={{ color: 'var(--primary)' }} onClick={() => selectActiveEvent(ev.id)}>
                          Ativar
                        </button>
                      )}
                      <button className="icon-btn" style={{ color: 'var(--danger)' }} onClick={() => setDeleteTarget(ev)}>
                        <Trash2 size={17} strokeWidth={2.2} />
                      </button>
                    </div>
                  </div>
                  <hr style={{ border: 'none', borderTop: '1px solid var(--outline-variant)', margin: 0 }} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {deleteTarget && (
        <div className="overlay" onClick={() => setDeleteTarget(null)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <h2>Excluir Evento?</h2>
            <p>
              Tem certeza que deseja excluir '{deleteTarget.title}'? Todos os participantes e gastos
              associados também serão apagados.
            </p>
            <div className="dialog-actions">
              <button className="btn btn-outline" onClick={() => setDeleteTarget(null)}>
                Cancelar
              </button>
              <button
                className="btn"
                style={{ background: 'var(--danger)', color: 'white' }}
                onClick={() => {
                  deleteEvent(deleteTarget)
                  setDeleteTarget(null)
                }}
              >
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
