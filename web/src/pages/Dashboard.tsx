import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { ComponentType } from 'react'
import {
  ChevronDown,
  CalendarCog,
  Printer,
  Send,
  Users,
  ShoppingCart,
  Wallet,
  MessageCircle,
  PartyPopper,
  Plus,
  Check,
  CalendarDays,
  LogOut,
} from 'lucide-react'
import { useAuth } from '../context/authContextValue'
import type { PartyStore } from '../store/usePartyStore'
import CountdownCard from '../components/CountdownCard'
import FinancialSummaryCard from '../components/FinancialSummaryCard'
import BudgetVsSpentChart from '../components/BudgetVsSpentChart'
import LocationMap from '../components/LocationMap'
import { formatCurrency, formatDateOnly } from '../utils/format'

function ShortcutCard({
  title,
  subtitle,
  icon: Icon,
  bg,
  color,
  onClick,
}: {
  title: string
  subtitle: string
  icon: ComponentType<{ size?: number; strokeWidth?: number }>
  bg: string
  color: string
  onClick: () => void
}) {
  return (
    <button className="shortcut-card" onClick={onClick} style={{ flex: 1 }}>
      <div className="icon-box" style={{ background: bg, color }}>
        <Icon size={19} strokeWidth={2.3} />
      </div>
      <div>
        <p className="shortcut-title">{title}</p>
        <p className="shortcut-subtitle">{subtitle}</p>
      </div>
    </button>
  )
}

export default function Dashboard({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const { signOutUser } = useAuth()
  const [showSelector, setShowSelector] = useState(false)
  const { activeEvent, events, financialSummary, expenses, selectActiveEvent } = store

  return (
    <>
      <div className="top-bar">
        <div style={{ flex: 1 }}>
          <h1>{activeEvent?.title ?? 'Festas & Eventos'}</h1>
          {activeEvent && (
            <p className="subtitle" style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <PartyPopper size={12} strokeWidth={2.4} /> {activeEvent.eventType} • {activeEvent.location}
            </p>
          )}
        </div>
        {events.length > 0 && (
          <button className="icon-btn" onClick={() => setShowSelector(true)} title="Selecionar Evento">
            <ChevronDown size={19} strokeWidth={2.4} />
          </button>
        )}
        <button className="icon-btn" onClick={() => navigate('/event-setup')} title="Configurar Evento">
          <CalendarCog size={19} strokeWidth={2.2} />
        </button>
        <button className="icon-btn" onClick={() => window.print()} title="Exportar / Imprimir">
          <Printer size={19} strokeWidth={2.2} />
        </button>
        <button className="icon-btn" onClick={signOutUser} title="Sair da conta">
          <LogOut size={19} strokeWidth={2.2} />
        </button>
      </div>

      <div className="app-content">
        {!activeEvent ? (
          <div className="empty-state">
            <span className="emoji">
              <PartyPopper size={30} strokeWidth={2} />
            </span>
            <p>Nenhum evento ativo no momento.</p>
            <button className="btn btn-primary" onClick={() => navigate('/event-setup')}>
              Criar Novo Evento
            </button>
          </div>
        ) : (
          <div className="page">
            <CountdownCard event={activeEvent} />

            <LocationMap
              location={activeEvent.location}
              latitude={activeEvent.latitude}
              longitude={activeEvent.longitude}
            />

            <div className="card" style={{ background: 'linear-gradient(155deg, rgba(255,255,255,0.7), rgba(255,227,241,0.55))' }}>
              <p className="card-title">Ações Rápidas & Exportação</p>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
                <button className="btn btn-outline" style={{ flex: 1 }} onClick={() => window.print()}>
                  <Printer size={16} strokeWidth={2.3} /> Imprimir
                </button>
                <button
                  className="btn btn-whatsapp"
                  style={{ flex: 1 }}
                  onClick={() => navigate('/invitations')}
                >
                  <Send size={16} strokeWidth={2.3} /> Convites
                </button>
              </div>
            </div>

            <FinancialSummaryCard summary={financialSummary} />

            <BudgetVsSpentChart budget={activeEvent.budget} expenses={expenses} />

            <p style={{ fontWeight: 800, margin: '8px 0 0' }}>Ações Rápidas & Módulos</p>

            <div className="grid-2">
              <ShortcutCard
                title="Convidados"
                subtitle={`${financialSummary.totalParticipants} Cadastrados`}
                icon={Users}
                bg="linear-gradient(155deg, #ffffff, #dce8ff)"
                color="#3b6fe0"
                onClick={() => navigate('/participants')}
              />
              <ShortcutCard
                title="Gastos"
                subtitle={formatCurrency(financialSummary.totalSpent)}
                icon={ShoppingCart}
                bg="linear-gradient(155deg, #ffffff, #d6f0ff)"
                color="#0891b2"
                onClick={() => navigate('/expenses')}
              />
            </div>
            <div className="grid-2">
              <ShortcutCard
                title="Rateio & Receber"
                subtitle={`Falta ${formatCurrency(financialSummary.missingCollection)}`}
                icon={Wallet}
                bg="linear-gradient(155deg, #ffffff, #d6f7e6)"
                color="#1f9e5c"
                onClick={() => navigate('/payments')}
              />
              <ShortcutCard
                title="Convites WhatsApp"
                subtitle="Modelos & Envio"
                icon={MessageCircle}
                bg="linear-gradient(155deg, #ffffff, #ffe3f1)"
                color="#c2277d"
                onClick={() => navigate('/invitations')}
              />
            </div>
          </div>
        )}
      </div>

      {showSelector && (
        <div className="overlay" onClick={() => setShowSelector(false)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <h2>Meus Eventos</h2>

            <button
              className="btn btn-primary btn-block"
              style={{ marginBottom: 14 }}
              onClick={() => {
                setShowSelector(false)
                navigate('/event-setup?new=1')
              }}
            >
              <Plus size={16} strokeWidth={2.4} /> Adicionar Novo Evento
            </button>

            {events.map((ev) => {
              const isActive = ev.id === activeEvent?.id
              return (
                <button
                  key={ev.id}
                  onClick={() => {
                    selectActiveEvent(ev.id)
                    setShowSelector(false)
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    width: '100%',
                    textAlign: 'left',
                    border: isActive ? '1.5px solid var(--primary)' : '1px solid var(--outline-variant)',
                    background: isActive
                      ? 'linear-gradient(155deg, rgba(139,108,240,0.14), rgba(139,108,240,0.06))'
                      : 'rgba(255,255,255,0.5)',
                    borderRadius: 16,
                    padding: '12px 14px',
                    marginBottom: 8,
                    cursor: 'pointer',
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <p style={{ margin: 0, fontWeight: 700, fontSize: '0.9rem' }}>{ev.title}</p>
                    <p
                      style={{
                        margin: '2px 0 0',
                        fontSize: '0.75rem',
                        color: 'var(--on-surface-variant)',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 4,
                      }}
                    >
                      <CalendarDays size={12} /> {formatDateOnly(ev.eventDateMillis)} • {formatCurrency(ev.budget)}
                    </p>
                  </div>
                  {isActive && (
                    <span
                      style={{
                        width: 24,
                        height: 24,
                        borderRadius: '50%',
                        background: 'var(--primary)',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      <Check size={14} strokeWidth={3} />
                    </span>
                  )}
                </button>
              )
            })}

            <div className="dialog-actions">
              <button className="btn btn-outline btn-block" onClick={() => setShowSelector(false)}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
