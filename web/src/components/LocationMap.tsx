import { useState } from 'react'
import { MapPin, ExternalLink, ChevronDown } from 'lucide-react'
import { googleMapsSearchUrl, appleMapsUrl, googleMapsEmbedUrl } from '../utils/links'

interface LocationMapProps {
  location: string
  latitude?: number
  longitude?: number
  defaultMinimized?: boolean
}

export default function LocationMap({
  location,
  latitude,
  longitude,
  defaultMinimized = true,
}: LocationMapProps) {
  const [minimized, setMinimized] = useState(defaultMinimized)

  if (!location.trim() && (latitude === undefined || longitude === undefined)) return null

  const coords = latitude !== undefined && longitude !== undefined ? { lat: latitude, lng: longitude } : undefined

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <button
        type="button"
        onClick={() => setMinimized((v) => !v)}
        style={{
          width: '100%',
          background: 'transparent',
          border: 'none',
          cursor: 'pointer',
          textAlign: 'left',
          padding: '16px 16px 10px',
          display: 'flex',
          alignItems: 'flex-start',
          gap: 8,
        }}
      >
        <div style={{ flex: 1 }}>
          <p className="card-title" style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 6 }}>
            <MapPin size={16} strokeWidth={2.3} color="var(--primary)" />
            Localização do Evento
          </p>
          <p style={{ fontSize: '0.8rem', color: 'var(--on-surface-variant)', margin: '4px 0 0', fontWeight: 600 }}>
            {location}
          </p>
        </div>
        <ChevronDown
          size={18}
          strokeWidth={2.4}
          style={{
            marginTop: 2,
            transition: 'transform 0.2s ease',
            transform: minimized ? 'rotate(0deg)' : 'rotate(180deg)',
          }}
        />
      </button>

      {!minimized && (
        <>
          <iframe
            title="Mapa do evento"
            src={googleMapsEmbedUrl(location, coords)}
            width="100%"
            height="170"
            style={{ border: 0, display: 'block' }}
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
          />

          <div style={{ display: 'flex', gap: 8, padding: 12 }}>
            <a
              className="btn btn-outline"
              style={{ flex: 1, textDecoration: 'none' }}
              href={googleMapsSearchUrl(location, coords)}
              target="_blank"
              rel="noopener noreferrer"
            >
              <ExternalLink size={14} strokeWidth={2.3} /> Google Maps
            </a>
            <a
              className="btn btn-outline"
              style={{ flex: 1, textDecoration: 'none' }}
              href={appleMapsUrl(location, coords)}
              target="_blank"
              rel="noopener noreferrer"
            >
              <ExternalLink size={14} strokeWidth={2.3} /> Apple Maps
            </a>
          </div>
        </>
      )}
    </div>
  )
}
