import { useEffect, useState } from 'react'
import { ChevronDown, MapPin, Link2, Search, LocateFixed, ExternalLink, Loader2 } from 'lucide-react'
import PinMap from './PinMap'
import { googleMapsSearchUrl, appleMapsUrl } from '../utils/links'
import { geocodeAddress, isShortGoogleMapsLink, parseGoogleMapsCoords } from '../utils/geo'

const DEFAULT_LAT = -23.5505
const DEFAULT_LNG = -46.6333

const BR_STATES = [
  ['AC', 'Acre'], ['AL', 'Alagoas'], ['AP', 'Amapá'], ['AM', 'Amazonas'],
  ['BA', 'Bahia'], ['CE', 'Ceará'], ['DF', 'Distrito Federal'], ['ES', 'Espírito Santo'],
  ['GO', 'Goiás'], ['MA', 'Maranhão'], ['MT', 'Mato Grosso'], ['MS', 'Mato Grosso do Sul'],
  ['MG', 'Minas Gerais'], ['PA', 'Pará'], ['PB', 'Paraíba'], ['PR', 'Paraná'],
  ['PE', 'Pernambuco'], ['PI', 'Piauí'], ['RJ', 'Rio de Janeiro'], ['RN', 'Rio Grande do Norte'],
  ['RS', 'Rio Grande do Sul'], ['RO', 'Rondônia'], ['RR', 'Roraima'], ['SC', 'Santa Catarina'],
  ['SP', 'São Paulo'], ['SE', 'Sergipe'], ['TO', 'Tocantins'],
] as const

interface AddressFields {
  street: string
  number: string
  neighborhood: string
  city: string
  state: string
}

function composeAddress({ street, number, neighborhood, city, state }: AddressFields): string {
  const streetPart = [street.trim(), number.trim()].filter(Boolean).join(', ')
  const cityStatePart = [city.trim(), state.trim()].filter(Boolean).join(' - ')
  const middlePart = [neighborhood.trim(), cityStatePart].filter(Boolean).join(', ')
  return [streetPart, middlePart].filter(Boolean).join(' - ')
}

interface EventLocationCardProps {
  location: string
  onLocationChange: (value: string) => void
  latitude?: number
  longitude?: number
  onCoordsChange: (lat: number, lng: number) => void
}

export default function EventLocationCard({
  location,
  onLocationChange,
  latitude,
  longitude,
  onCoordsChange,
}: EventLocationCardProps) {
  const [expanded, setExpanded] = useState(true)
  const [mapsLinkText, setMapsLinkText] = useState('')
  const [linkError, setLinkError] = useState<string | null>(null)
  const [geocoding, setGeocoding] = useState(false)
  const [geocodeError, setGeocodeError] = useState<string | null>(null)

  const [street, setStreet] = useState('')
  const [number, setNumber] = useState('')
  const [neighborhood, setNeighborhood] = useState('')
  const [city, setCity] = useState('')
  const [state, setState] = useState('')

  useEffect(() => {
    const composed = composeAddress({ street, number, neighborhood, city, state })
    if (composed) onLocationChange(composed)
    // onLocationChange is a setter passed down from the parent; including it would refire on every parent render.
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [street, number, neighborhood, city, state])

  const lat = latitude ?? DEFAULT_LAT
  const lng = longitude ?? DEFAULT_LNG
  const hasPin = latitude !== undefined && longitude !== undefined
  const coords = hasPin ? { lat, lng } : undefined
  const hasStructuredInput = Boolean(street || number || neighborhood || city || state)

  const handleParseLink = () => {
    setLinkError(null)
    if (!mapsLinkText.trim()) return

    if (isShortGoogleMapsLink(mapsLinkText)) {
      setLinkError(
        'Links curtos (maps.app.goo.gl) não podem ser lidos direto pelo navegador. Abra o link, copie o endereço completo que aparece na barra do navegador e cole aqui.',
      )
      return
    }

    const parsed = parseGoogleMapsCoords(mapsLinkText)
    if (!parsed) {
      setLinkError('Não encontramos coordenadas nesse link. Copie o link completo da página do local no Google Maps.')
      return
    }
    onCoordsChange(parsed.lat, parsed.lng)
  }

  const handleGeocode = async () => {
    if (!location.trim()) return
    setGeocoding(true)
    setGeocodeError(null)
    try {
      const found = await geocodeAddress(location)
      if (found) {
        onCoordsChange(found.lat, found.lng)
      } else {
        setGeocodeError('Endereço não encontrado. Tente ser mais específico ou ajuste o pin manualmente no mapa.')
      }
    } catch {
      setGeocodeError('Não foi possível buscar o endereço agora. Verifique sua conexão.')
    } finally {
      setGeocoding(false)
    }
  }

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          padding: 16,
          background: 'transparent',
          border: 'none',
          cursor: 'pointer',
          textAlign: 'left',
        }}
      >
        <MapPin size={17} strokeWidth={2.3} color="var(--primary)" />
        <span style={{ flex: 1, fontWeight: 800, fontSize: '1rem' }}>Localização do Evento</span>
        <ChevronDown
          size={18}
          strokeWidth={2.4}
          style={{ transition: 'transform 0.2s ease', transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)' }}
        />
      </button>

      {expanded && (
        <div style={{ padding: '0 16px 16px' }}>
          <label className="field-label">Endereço Completo</label>

          {location && !hasStructuredInput && (
            <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: '0 0 8px' }}>
              Endereço atual salvo: <strong>{location}</strong>. Preencha os campos abaixo para atualizar.
            </p>
          )}

          <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
            <input
              type="text"
              value={street}
              onChange={(e) => setStreet(e.target.value)}
              placeholder="Rua / Avenida"
              style={{ flex: 2 }}
            />
            <input
              type="text"
              value={number}
              onChange={(e) => setNumber(e.target.value)}
              placeholder="Número"
              style={{ flex: 1 }}
            />
          </div>

          <div style={{ marginBottom: 10 }}>
            <input
              type="text"
              value={neighborhood}
              onChange={(e) => setNeighborhood(e.target.value)}
              placeholder="Bairro"
            />
          </div>

          <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
            <input
              type="text"
              value={city}
              onChange={(e) => setCity(e.target.value)}
              placeholder="Cidade"
              style={{ flex: 2 }}
            />
            <select value={state} onChange={(e) => setState(e.target.value)} style={{ flex: 1 }}>
              <option value="">UF</option>
              {BR_STATES.map(([uf, name]) => (
                <option key={uf} value={uf}>
                  {name}
                </option>
              ))}
            </select>
          </div>

          {location && (
            <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: '0 0 10px' }}>
              Endereço: <strong style={{ color: 'var(--on-surface)' }}>{location}</strong>
            </p>
          )}

          <button
            type="button"
            className="btn btn-outline btn-block"
            onClick={handleGeocode}
            disabled={geocoding || !location.trim()}
            style={{ marginBottom: 12 }}
          >
            {geocoding ? <Loader2 size={16} strokeWidth={2.2} className="spin" /> : <Search size={16} strokeWidth={2.2} />}
            Buscar endereço e posicionar o pin
          </button>
          {geocodeError && (
            <p style={{ color: 'var(--danger)', fontSize: '0.72rem', marginTop: -8, marginBottom: 10 }}>{geocodeError}</p>
          )}

          <div className="field">
            <label className="field-label">Colar link do Google Maps</label>
            <div style={{ display: 'flex', gap: 8 }}>
              <input
                type="text"
                value={mapsLinkText}
                onChange={(e) => {
                  setMapsLinkText(e.target.value)
                  setLinkError(null)
                }}
                placeholder="Cole aqui o link completo do Google Maps"
              />
              <button
                type="button"
                className="icon-btn"
                title="Localizar pin pelo link"
                onClick={handleParseLink}
                style={{ flexShrink: 0 }}
              >
                <Link2 size={17} strokeWidth={2.2} />
              </button>
            </div>
            {linkError && <p style={{ color: 'var(--danger)', fontSize: '0.72rem', marginTop: 6 }}>{linkError}</p>}
          </div>

          <label className="field-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <LocateFixed size={14} /> Ajuste fino: toque ou arraste o pin no mapa
          </label>
          <div style={{ borderRadius: 16, overflow: 'hidden', border: '1px solid var(--outline-variant)' }}>
            <PinMap lat={lat} lng={lng} onChange={onCoordsChange} />
          </div>

          {hasPin && (
            <p style={{ fontSize: '0.7rem', color: 'var(--on-surface-variant)', marginTop: 6 }}>
              Pin definido em {lat.toFixed(5)}, {lng.toFixed(5)}
            </p>
          )}

          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
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
      )}
    </div>
  )
}
