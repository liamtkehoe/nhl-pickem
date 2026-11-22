import { useEffect, useState, type ReactNode } from 'react'
import { THEME_STORAGE_KEY, ThemeContext, savedTheme, type Theme } from './theme'

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [theme, setTheme] = useState<Theme>(savedTheme)

    useEffect(() => {
        document.documentElement.dataset.theme = theme
        try {
            localStorage.setItem(THEME_STORAGE_KEY, theme)
        } catch {

        }
    }, [theme])

    function toggle() {
        setTheme(current => (current === 'dark' ? 'light' : 'dark'))
    }

    return <ThemeContext.Provider value={{ theme, toggle }}>{children}</ThemeContext.Provider>
}