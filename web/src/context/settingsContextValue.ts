import { createContext, useContext } from 'react'

export type ThemeId = 'violeta' | 'oceano' | 'esmeralda' | 'coral'
export type FontId =
  | 'system'
  | 'roboto-flex'
  | 'inter'
  | 'poppins'
  | 'nunito'
  | 'quicksand'
  | 'montserrat'
  | 'lato'
  | 'playfair-display'
  | 'comfortaa'

export interface ThemeOption {
  id: ThemeId
  label: string
  swatch: string
}

export const THEMES: ThemeOption[] = [
  { id: 'violeta', label: 'Violeta', swatch: '#8b6cf0' },
  { id: 'oceano', label: 'Oceano', swatch: '#3b82f6' },
  { id: 'esmeralda', label: 'Esmeralda', swatch: '#10b981' },
  { id: 'coral', label: 'Coral', swatch: '#fb7185' },
]

export interface FontOption {
  id: FontId
  label: string
  stack: string
}

export const FONTS: FontOption[] = [
  { id: 'system', label: 'Padrão do Sistema', stack: "'Segoe UI', system-ui, -apple-system, sans-serif" },
  { id: 'roboto-flex', label: 'Roboto Flex', stack: "'Roboto Flex Variable', sans-serif" },
  { id: 'inter', label: 'Inter', stack: "'Inter', sans-serif" },
  { id: 'poppins', label: 'Poppins', stack: "'Poppins', sans-serif" },
  { id: 'nunito', label: 'Nunito', stack: "'Nunito', sans-serif" },
  { id: 'quicksand', label: 'Quicksand', stack: "'Quicksand', sans-serif" },
  { id: 'montserrat', label: 'Montserrat', stack: "'Montserrat', sans-serif" },
  { id: 'lato', label: 'Lato', stack: "'Lato', sans-serif" },
  { id: 'playfair-display', label: 'Playfair Display', stack: "'Playfair Display', serif" },
  { id: 'comfortaa', label: 'Comfortaa', stack: "'Comfortaa', sans-serif" },
]

export interface SettingsContextValue {
  theme: ThemeId
  setTheme: (theme: ThemeId) => void
  font: FontId
  setFont: (font: FontId) => void
}

export const SettingsContext = createContext<SettingsContextValue | null>(null)

export function useSettings() {
  const ctx = useContext(SettingsContext)
  if (!ctx) throw new Error('useSettings must be used within SettingsProvider')
  return ctx
}
