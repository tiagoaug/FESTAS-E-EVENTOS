import { useEffect, useState } from 'react'
import { Timer, CalendarDays, MapPin, CalendarPlus } from 'lucide-react'
import type { EventEntity } from '../types'
import { formatDate } from '../utils/format'
import { googleCalendarUrl } from '../utils/links'

function pad(n: number) {
  return n.toString().padStart(2, '0')
}

export default function CountdownCard({ event }: { event: EventEntity }) {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])

  const diff = event.eventDateMillis - now
  const isPast = diff <= 0
  const days = isPast ? 0 : Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = isPast ? 0 : Math.floor(diff / (1000 * 60 * 60)) % 24
  const minutes = isPast ? 0 : Math.floor(diff / (1000 * 60)) % 60
  const seconds = isPast ? 0 : Math.floor(diff / 1000) % 60

  return (
    <div
      style={{
        position: 'relative',
        borderRadius: 26,
        overflow: 'hidden',
        background: 'linear-gradient(160deg, #9d7bf0 0%, #a978d8 35%, #7c6ce0 70%, #6c8ef0 100%)',
        color: 'white',
        padding: 22,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        textAlign: 'center',
        border: '1px solid rgba(255,255,255,0.35)',
        boxShadow: '0 20px 40px -14px rgba(110, 80, 220, 0.55), 0 1px 0 rgba(255,255,255,0.5) inset',
      }}
    >
      <div
        aria-hidden
        style={{
          position: 'absolute',
          top: -60,
          right: -60,
          width: 160,
          height: 160,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(255,255,255,0.35), transparent 70%)',
        }}
      />
      <div
        aria-hidden
        style={{
          position: 'absolute',
          bottom: -80,
          left: -50,
          width: 180,
          height: 180,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(255,255,255,0.18), transparent 70%)',
        }}
      />

      <span
        style={{
          background: 'rgba(255,255,255,0.25)',
          border: '1px solid rgba(255,255,255,0.35)',
          borderRadius: 50,
          padding: '6px 14px',
          fontSize: '0.72rem',
          fontWeight: 800,
          letterSpacing: 0.6,
          display: 'inline-flex',
          alignItems: 'center',
          gap: 6,
        }}
      >
        <Timer size={14} strokeWidth={2.5} />
        {isPast ? 'FESTA REALIZADA!' : 'FALTAM POUCOS DIAS!'}
      </span>

      <h2 style={{ fontSize: '1.4rem', fontWeight: 800, margin: '14px 0 4px', letterSpacing: -0.3 }}>
        {event.title}
      </h2>

      <p style={{ fontSize: '0.85rem', opacity: 0.95, margin: '2px 0', display: 'flex', alignItems: 'center', gap: 6 }}>
        <CalendarDays size={14} /> {formatDate(event.eventDateMillis)}
      </p>
      {event.location && (
        <p style={{ fontSize: '0.78rem', opacity: 0.85, margin: '2px 0', display: 'flex', alignItems: 'center', gap: 6 }}>
          <MapPin size={13} /> {event.location}
        </p>
      )}

      <div style={{ display: 'flex', gap: 10, marginTop: 18, width: '100%', justifyContent: 'space-evenly' }}>
        {[
          { value: days, label: 'DIAS' },
          { value: hours, label: 'HORAS' },
          { value: minutes, label: 'MINS' },
          { value: seconds, label: 'SEGS' },
        ].map((b) => (
          <div
            key={b.label}
            style={{
              background: 'rgba(255,255,255,0.18)',
              border: '1px solid rgba(255,255,255,0.3)',
              borderRadius: 16,
              padding: '10px 12px',
              minWidth: 52,
              boxShadow: '0 4px 10px -4px rgba(0,0,0,0.2), 0 1px 0 rgba(255,255,255,0.3) inset',
            }}
          >
            <div style={{ fontSize: '1.3rem', fontWeight: 800 }}>{pad(b.value)}</div>
            <div style={{ fontSize: '0.6rem', opacity: 0.85, fontWeight: 700, letterSpacing: 0.5 }}>{b.label}</div>
          </div>
        ))}
      </div>

      <a
        href={googleCalendarUrl(event.title, event.location, event.eventDateMillis)}
        target="_blank"
        rel="noopener noreferrer"
        className="btn"
        style={{
          marginTop: 18,
          background: 'linear-gradient(155deg, #ffe08a, #ffb84d)',
          color: '#5b3a00',
          textDecoration: 'none',
          boxShadow: '0 8px 18px -6px rgba(255, 184, 77, 0.6), 0 1px 0 rgba(255,255,255,0.5) inset',
        }}
      >
        <CalendarPlus size={16} strokeWidth={2.5} />
        Adicionar à Google Agenda
      </a>
    </div>
  )
}
