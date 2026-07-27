import { useEffect, useState, type ReactNode } from 'react'
import { FONTS, SettingsContext, type FontId, type ThemeId } from './settingsContextValue'

const THEME_KEY = 'festas-eventos-theme'
const FONT_KEY = 'festas-eventos-font'

function readStored<T extends string>(key: string, fallback: T): T {
  try {
    return (localStorage.getItem(key) as T) || fallback
  } catch {
    return fallback
  }
}

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<ThemeId>(() => readStored<ThemeId>(THEME_KEY, 'violeta'))
  const [font, setFont] = useState<FontId>(() => readStored<FontId>(FONT_KEY, 'system'))

  useEffect(() => {
    document.documentElement.setAttribute('data-app-theme', theme)
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  useEffect(() => {
    const fontDef = FONTS.find((f) => f.id === font) ?? FONTS[0]
    document.documentElement.style.fontFamily = fontDef.stack
    localStorage.setItem(FONT_KEY, font)
  }, [font])

  return (
    <SettingsContext.Provider value={{ theme, setTheme, font, setFont }}>{children}</SettingsContext.Provider>
  )
}
