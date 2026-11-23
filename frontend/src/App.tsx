import { useCallback, useEffect, useState } from 'react'
import { api, setCurrentUser, type LoginResponse, type SeasonInfo } from './api'
import { AdminPage } from './admin/AdminPage'
import { Dashboard } from './dashboard/Dashboard'
import { LoginScreen } from './LoginScreen'
import { PicksWizard } from './picks/PicksWizard'
import { ThemeToggle } from './ThemeToggle'

type Screen = 'login' | 'picks' | 'dashboard' | 'admin'

export default function App() {
    const [user, setUser] = useState<LoginResponse | null>(null)
    const [screen, setScreen] = useState<Screen>('login')
    const [season, setSeason] = useState<SeasonInfo | null>(null)

    const loadSeason = useCallback(() => {
        api.season().then(setSeason).catch(() => setSeason(null))
    }, [])

    useEffect(() => {
        loadSeason()
    }, [loadSeason])

    function enter(loggedIn: LoginResponse) {
        setCurrentUser(loggedIn.username)
        setUser(loggedIn)
        setScreen(loggedIn.submittedAt ? 'dashboard' : 'picks')
    }

    function switchPlayer() {
        setCurrentUser(null)
        setUser(null)
        setScreen('login')
    }

    function goHome() {
        setScreen(user?.submittedAt ? 'dashboard' : 'picks')
    }

    return (
        <div className="app">
            <ThemeToggle />
            {user && screen !== 'login' && (
                <header className="topbar">
                    <div className="brand">
                        <button type="button" className="link brand-link" onClick={goHome}>
                            <strong>NHL Pick'em</strong>
                        </button>
                        {season && (
                            <span className="season-badge">
                {season.label}
                                {season.practice ? ' · Practice' : ''}
              </span>
                        )}
                    </div>
                    <span>
            {user.admin && screen !== 'admin' && (
                <>
                    <button type="button" className="link" onClick={() => setScreen('admin')}>
                        Admin
                    </button>
                    {' · '}
                </>
            )}
                        {user.username} ·{' '}
                        <button type="button" className="link" onClick={switchPlayer}>
              Switch player
            </button>
          </span>
                </header>
            )}
            <main>
                {screen === 'login' && <LoginScreen season={season} onLogin={enter} />}
                {user && screen === 'picks' && (
                    <PicksWizard
                        username={user.username}
                        onSubmitted={() => {
                            setUser({ ...user, submittedAt: new Date().toISOString() })
                            setScreen('dashboard')
                        }}
                    />
                )}
                {user && screen === 'dashboard' && <Dashboard username={user.username} season={season} />}
                {user && screen === 'admin' && (
                    <AdminPage me={user.username} season={season} onSeasonChanged={loadSeason} />
                )}
            </main>
        </div>
    )
}