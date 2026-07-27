export interface Coords {
  lat: number
  lng: number
}

export function isShortGoogleMapsLink(input: string): boolean {
  try {
    const url = new URL(input.trim())
    return /(^|\.)goo\.gl$/.test(url.hostname) || url.hostname === 'maps.app.goo.gl'
  } catch {
    return false
  }
}

export function parseGoogleMapsCoords(input: string): Coords | null {
  const text = input.trim()

  const plain = text.match(/^(-?\d{1,2}\.\d+),\s*(-?\d{1,3}\.\d+)$/)
  if (plain) return { lat: parseFloat(plain[1]), lng: parseFloat(plain[2]) }

  const bang = text.match(/!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)/)
  if (bang) return { lat: parseFloat(bang[1]), lng: parseFloat(bang[2]) }

  const at = text.match(/@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)/)
  if (at) return { lat: parseFloat(at[1]), lng: parseFloat(at[2]) }

  try {
    const url = new URL(text)
    const param = url.searchParams.get('q') || url.searchParams.get('ll') || url.searchParams.get('query')
    if (param) {
      const m = param.match(/^(-?\d{1,2}\.\d+),\s*(-?\d{1,3}\.\d+)$/)
      if (m) return { lat: parseFloat(m[1]), lng: parseFloat(m[2]) }
    }
  } catch {
    return null
  }

  return null
}

export async function geocodeAddress(address: string): Promise<Coords | null> {
  const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(address)}`
  const res = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!res.ok) return null
  const data: Array<{ lat: string; lon: string }> = await res.json()
  if (!Array.isArray(data) || data.length === 0) return null
  return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon) }
}
