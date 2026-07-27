import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Save, MessageCircle, Send, CheckCircle2, PartyPopper, MapPin, Navigation } from 'lucide-react'
import type { PartyStore } from '../store/usePartyStore'
import { calculateParticipantTarget } from '../store/usePartyStore'
import type { ParticipantEntity } from '../types'
import { formatCurrency, formatDate } from '../utils/format'
import { openWhatsAppMessage, googleMapsSearchUrl, appleMapsUrl } from '../utils/links'

const DEFAULT_TEMPLATE =
  'Olá {nome}! Você está convidado(a) para {evento} no dia {data} no local {local}. Sua contribuição: {valor}. Confirme sua presença!'

export default function Invitations({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const { activeEvent, participants, updateEvent } = store

  const [templateText, setTemplateText] = useState(activeEvent?.invitationTemplate ?? DEFAULT_TEMPLATE)
  const [saved, setSaved] = useState(false)
  const [locationShareTarget, setLocationShareTarget] = useState<ParticipantEntity | null>(null)

  const sampleMessage = useMemo(() => {
    const sampleName = 'Mariane Silva'
    const sampleEventName = activeEvent?.title ?? 'Aniversário de 30 Anos'
    const sampleDate = formatDate(activeEvent?.eventDateMillis ?? Date.now())
    const sampleLocation = activeEvent?.location || 'Espaço Festas & Cia'
    const sampleValue = formatCurrency(50)
    return templateText
      .replace('{nome}', sampleName)
      .replace('{evento}', sampleEventName)
      .replace('{data}', sampleDate)
      .replace('{local}', sampleLocation)
      .replace('{valor}', sampleValue)
  }, [templateText, activeEvent])

  if (!activeEvent) {
    return (
      <>
        <div className="top-bar">
          <button className="icon-btn" onClick={() => navigate('/')}>
            <ArrowLeft size={19} strokeWidth={2.3} />
          </button>
          <h1>Convites & WhatsApp</h1>
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

  const eventCoords =
    activeEvent.latitude !== undefined && activeEvent.longitude !== undefined
      ? { lat: activeEvent.latitude, lng: activeEvent.longitude }
      : undefined

  return (
    <>
      <div className="top-bar">
        <button className="icon-btn" onClick={() => navigate('/')}>
          <ArrowLeft size={19} strokeWidth={2.3} />
        </button>
        <h1>Convites & WhatsApp</h1>
      </div>

      <div className="app-content">
        <div className="page">
          <div className="card">
            <p className="card-title">Modelo de Mensagem de Convite</p>
            <p style={{ fontSize: '0.75rem', color: 'var(--on-surface-variant)', marginTop: -8 }}>
              Use as variáveis: {'{nome}'}, {'{evento}'}, {'{data}'}, {'{local}'}, {'{valor}'}
            </p>
            <textarea
              rows={4}
              value={templateText}
              onChange={(e) => {
                setTemplateText(e.target.value)
                setSaved(false)
              }}
            />
            <div className="chip-row" style={{ marginTop: 8 }}>
              {['{nome}', '{evento}', '{data}', '{local}', '{valor}'].map((tag) => (
                <button key={tag} className="chip" onClick={() => setTemplateText((t) => `${t} ${tag}`)}>
                  {tag}
                </button>
              ))}
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
              {saved ? (
                <span style={{ color: 'var(--success)', fontWeight: 700, fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: 4 }}>
                  <CheckCircle2 size={14} strokeWidth={2.4} /> Modelo salvo!
                </span>
              ) : (
                <span />
              )}
              <button
                className="btn btn-primary"
                onClick={() => {
                  updateEvent({ ...activeEvent, invitationTemplate: templateText })
                  setSaved(true)
                }}
              >
                <Save size={16} strokeWidth={2.3} /> Salvar Modelo
              </button>
            </div>
          </div>

          <div className="card" style={{ background: 'linear-gradient(155deg, rgba(223,250,235,0.85), rgba(223,250,235,0.55))' }}>
            <p style={{ fontWeight: 800, color: '#0d7a52', margin: '0 0 6px', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 6 }}>
              <MessageCircle size={16} strokeWidth={2.3} /> Pré-visualização da Mensagem (WhatsApp)
            </p>
            <p style={{ color: 'var(--on-surface)', margin: 0, fontSize: '0.88rem' }}>{sampleMessage}</p>
          </div>

          <p style={{ fontWeight: 800 }}>Enviar Convites Individuais ({participants.length} participantes)</p>

          {participants.map((p) => {
            const target = calculateParticipantTarget(p, activeEvent, participants)
            const message = templateText
              .replace('{nome}', p.name)
              .replace('{evento}', activeEvent.title)
              .replace('{data}', formatDate(activeEvent.eventDateMillis))
              .replace('{local}', activeEvent.location)
              .replace('{valor}', formatCurrency(target))

            return (
              <div key={p.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{ flex: 1 }}>
                  <strong style={{ fontSize: '0.88rem' }}>{p.name}</strong>
                  <p style={{ margin: '2px 0', fontSize: '0.75rem', color: 'var(--on-surface-variant)' }}>
                    Tel: {p.phone || 'Não informado'} • {p.familyGroup}
                  </p>
                </div>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button
                    className="icon-btn"
                    title="Compartilhar Localização"
                    onClick={() => setLocationShareTarget(p)}
                  >
                    <MapPin size={17} strokeWidth={2.2} />
                  </button>
                  <button className="btn btn-whatsapp" onClick={() => openWhatsAppMessage(p.phone, message)}>
                    <Send size={15} strokeWidth={2.3} /> WhatsApp
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {locationShareTarget && (
        <div className="overlay" onClick={() => setLocationShareTarget(null)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <h2>Compartilhar Localização</h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--on-surface-variant)' }}>
              Enviar o endereço de <strong>{activeEvent.location || activeEvent.title}</strong> para{' '}
              <strong>{locationShareTarget.name}</strong> via WhatsApp:
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 14 }}>
              <button
                className="btn btn-outline btn-block"
                onClick={() => {
                  const msg = `Localização de ${activeEvent.title}: ${activeEvent.location}\n${googleMapsSearchUrl(activeEvent.location, eventCoords)}`
                  openWhatsAppMessage(locationShareTarget.phone, msg)
                  setLocationShareTarget(null)
                }}
              >
                <Navigation size={16} strokeWidth={2.3} /> Enviar via Google Maps
              </button>
              <button
                className="btn btn-outline btn-block"
                onClick={() => {
                  const msg = `Localização de ${activeEvent.title}: ${activeEvent.location}\n${appleMapsUrl(activeEvent.location, eventCoords)}`
                  openWhatsAppMessage(locationShareTarget.phone, msg)
                  setLocationShareTarget(null)
                }}
              >
                <Navigation size={16} strokeWidth={2.3} /> Enviar via Apple Maps
              </button>
            </div>
            <div className="dialog-actions">
              <button className="btn btn-outline" onClick={() => setLocationShareTarget(null)}>
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
