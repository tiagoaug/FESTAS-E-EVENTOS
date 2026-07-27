import { MapPin, ExternalLink } from 'lucide-react'
import { googleMapsSearchUrl, appleMapsUrl, googleMapsEmbedUrl } from '../utils/links'

interface LocationMapProps {
  location: string
  latitude?: number
  longitude?: number
}

export default function LocationMap({ location, latitude, longitude }: LocationMapProps) {
  if (!location.trim() && (latitude === undefined || longitude === undefined)) return null

  const coords = latitude !== undefined && longitude !== undefined ? { lat: latitude, lng: longitude } : undefined

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ padding: '16px 16px 10px' }}>
        <p
          className="card-title"
          style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 6 }}
        >
          <MapPin size={16} strokeWidth={2.3} color="var(--primary)" />
          Localização do Evento
        </p>
        <p style={{ fontSize: '0.8rem', color: 'var(--on-surface-variant)', margin: '4px 0 0', fontWeight: 600 }}>
          {location}
        </p>
      </div>

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
    </div>
  )
}
