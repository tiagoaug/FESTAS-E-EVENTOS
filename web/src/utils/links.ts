import type { Coords } from './geo'

export function openWhatsAppMessage(phone: string, message: string) {
  const digits = phone.replace(/\D/g, '')
  const withCountryCode = digits.length > 0 && digits.length <= 11 ? `55${digits}` : digits
  const url = `https://wa.me/${withCountryCode}?text=${encodeURIComponent(message)}`
  window.open(url, '_blank', 'noopener,noreferrer')
}

export function googleCalendarUrl(title: string, location: string, startMillis: number): string {
  const start = new Date(startMillis)
  const end = new Date(startMillis + 3 * 60 * 60 * 1000)
  const fmt = (d: Date) => d.toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z'
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: title,
    dates: `${fmt(start)}/${fmt(end)}`,
    location,
  })
  return `https://calendar.google.com/calendar/render?${params.toString()}`
}

export function googleMapsSearchUrl(location: string, coords?: Coords): string {
  const query = coords ? `${coords.lat},${coords.lng}` : location
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`
}

export function appleMapsUrl(location: string, coords?: Coords): string {
  const query = coords ? `${coords.lat},${coords.lng}` : location
  return `https://maps.apple.com/?q=${encodeURIComponent(query)}`
}

export function googleMapsEmbedUrl(location: string, coords?: Coords): string {
  const query = coords ? `${coords.lat},${coords.lng}` : location
  return `https://www.google.com/maps?q=${encodeURIComponent(query)}&output=embed`
}
