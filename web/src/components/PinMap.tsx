import { useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const pinIcon = L.divIcon({
  className: 'pin-marker-icon',
  html: `<svg width="34" height="42" viewBox="0 0 34 42" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 4px 6px rgba(90,60,160,0.45));">
    <defs>
      <linearGradient id="pinGrad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#a98cf5"/>
        <stop offset="100%" stop-color="#7657dd"/>
      </linearGradient>
    </defs>
    <path d="M17 0C7.6 0 0 7.6 0 17c0 12.7 17 25 17 25s17-12.3 17-25C34 7.6 26.4 0 17 0z" fill="url(#pinGrad)"/>
    <circle cx="17" cy="17" r="7" fill="white"/>
  </svg>`,
  iconSize: [34, 42],
  iconAnchor: [17, 42],
})

interface PinMapProps {
  lat: number
  lng: number
  onChange: (lat: number, lng: number) => void
}

export default function PinMap({ lat, lng, onChange }: PinMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const markerRef = useRef<L.Marker | null>(null)
  const onChangeRef = useRef(onChange)
  onChangeRef.current = onChange

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return

    const map = L.map(containerRef.current).setView([lat, lng], 15)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(map)

    const marker = L.marker([lat, lng], { draggable: true, icon: pinIcon }).addTo(map)
    marker.on('dragend', () => {
      const pos = marker.getLatLng()
      onChangeRef.current(pos.lat, pos.lng)
    })
    map.on('click', (e: L.LeafletMouseEvent) => {
      marker.setLatLng(e.latlng)
      onChangeRef.current(e.latlng.lat, e.latlng.lng)
    })

    mapRef.current = map
    markerRef.current = marker

    return () => {
      map.remove()
      mapRef.current = null
      markerRef.current = null
    }
    // Runs once: lat/lng only seed the initial view, later updates are handled by the effect below.
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (mapRef.current && markerRef.current) {
      markerRef.current.setLatLng([lat, lng])
      mapRef.current.setView([lat, lng], mapRef.current.getZoom())
    }
  }, [lat, lng])

  return <div ref={containerRef} style={{ height: 220, width: '100%' }} />
}
