import { useEffect, useState } from 'react'
import { api, setCurrentUser, type LoginResponse, type SeasonInfo } from './api'
import { LoginScreen } from './LoginScreen'
import { PicksWizard } from './picks/PicksWizard'
import { Dashboard } from './dashboard/Dashboard'
import { ThemeToggle } from './ThemeToggle'

type Screen =
    | { name: 'login' }
    | { name: 'picks'; username: string }
    | { name: 'dashboard'; username: string }

export default function App() {
    const [screen, setScreen] = useState<Screen>({ name: 'login' })
    const [season, setSeason] = useState<SeasonInfo | null>(null)

    useEffect(() => {
        api.season().then(setSeason).catch(() => setSeason(null))
    }, [])

    function enter(user: LoginResponse) {
        setCurrentUser(user.username)
        setScreen(
            user.submittedAt
                ? { name: 'dashboard', username: user.username }
                : { name: 'picks', username: user.username },
        )
    }

    function switchPlayer() {
        setCurrentUser(null)
        setScreen({ name: 'login' })
    }

    return (
        <div className="app">
            <ThemeToggle />
            {screen.name !== 'login' && (
                <header className="topbar">
                    <div className="brand">
                        <strong>NHL Pick'em</strong>
                        {season && (
                            <span className="season-badge">
                {season.label}
                                {season.practice ? ' · Practice' : ''}
              </span>
                        )}
                    </div>
                    <span>
            {screen.username} ·{' '}
                        <button type="button" className="link" onClick={switchPlayer}>
              Switch player
            </button>
          </span>
                </header>
            )}
            <main>
                {screen.name === 'login' && <LoginScreen season={season} onLogin={enter} />}
                {screen.name === 'picks' && (
                    <PicksWizard
                        username={screen.username}
                        onSubmitted={() => setScreen({ name: 'dashboard', username: screen.username })}
                    />
                )}
                {screen.name === 'dashboard' && <Dashboard username={screen.username} season={season} />}
            </main>
        </div>
    )
}