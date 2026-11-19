import { useState } from 'react'
import { setCurrentUser, type LoginResponse } from './api'
import { LoginScreen } from './LoginScreen'
import { PicksWizard } from './picks/PicksWizard'
import { Dashboard } from './dashboard/Dashboard'

type Screen =
    | { name: 'login' }
    | { name: 'picks'; username: string }
    | { name: 'dashboard'; username: string }

export default function App() {
  const [screen, setScreen] = useState<Screen>({ name: 'login' })

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
        {screen.name !== 'login' && (
            <header className="topbar">
              <strong>NHL Pick'em</strong>
              <span>
            {screen.username} ·{' '}
                <button type="button" className="link" onClick={switchPlayer}>
              Switch player
            </button>
          </span>
            </header>
        )}
        <main>
          {screen.name === 'login' && <LoginScreen onLogin={enter} />}
          {screen.name === 'picks' && (
              <PicksWizard
                  username={screen.username}
                  onSubmitted={() => setScreen({ name: 'dashboard', username: screen.username })}
              />
          )}
            {screen.name === 'dashboard' && <Dashboard username={screen.username} />}
        </main>
      </div>
  )
}