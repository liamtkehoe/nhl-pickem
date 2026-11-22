import { createContext, useContext } from 'react'

export type Theme = 'dark' | 'light'

export const THEME_STORAGE_KEY = 'pickem-theme'

export interface ThemeValue {
    theme: Theme
    toggle: () => void
}

export const ThemeContext = createContext<ThemeValue>({ theme: 'dark', toggle: () => {} })

export function useTheme(): ThemeValue {
    return useContext(ThemeContext)
}

export function savedTheme(): Theme {
    try {
        return localStorage.getItem(THEME_STORAGE_KEY) === 'light' ? 'light' : 'dark'
    } catch {
        return 'dark'
    }
}