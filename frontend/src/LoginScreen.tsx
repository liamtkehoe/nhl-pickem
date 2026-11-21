import { useState } from 'react'
import { api, ApiError, type LoginResponse, type SeasonInfo } from './api'

type Mode = 'choose' | 'signup' | 'login'

interface Props {
    season: SeasonInfo | null
    onLogin: (user: LoginResponse) => void
}

export function LoginScreen({ season, onLogin }: Props) {
    const [mode, setMode] = useState<Mode>('choose')
    const [username, setUsername] = useState('')
    const [error, setError] = useState<string | null>(null)
    const [busy, setBusy] = useState(false)

    function switchTo(next: Mode) {
        setMode(next)
        setError(null)
    }

    async function submit() {
        setError(null)
        setBusy(true)
        try {
            onLogin(mode === 'signup' ? await api.signUp(username) : await api.login(username))
        } catch (e) {
            setError(errorMessage(e))
        } finally {
            setBusy(false)
        }
    }

    const signingUp = mode === 'signup'

    return (
        <div className="login">
            <h1>NHL Pick'em</h1>
            {season && (
                <p className="subtitle">
                    {season.label} season{season.practice ? ' (practice)' : ''}
                </p>
            )}

            {mode === 'choose' ? (
                <div className="choose">
                    <button type="button" className="primary" onClick={() => switchTo('login')}>
                        Log In
                    </button>
                    <button type="button" onClick={() => switchTo('signup')}>
                        Sign Up
                    </button>
                    <p className="hint">Please log in or sign up</p>
                </div>
            ) : (
                <form
                    className="login-form"
                    onSubmit={e => {
                        e.preventDefault()
                        void submit()
                    }}
                >
                    <h2>{signingUp ? 'Create an account' : 'Log in'}</h2>
                    <label htmlFor="username">Username</label>
                    <input
                        id="username"
                        value={username}
                        onChange={e => setUsername(e.target.value)}
                        autoComplete="username"
                        autoFocus
                    />
                    {signingUp && (
                        <p className="hint">3-20 letters, numbers, - or _. Capitalization doesn't matter when logging in.</p>
                    )}
                    <button type="submit" className="primary" disabled={busy || username.trim() === ''}>
                        {signingUp ? 'Create account' : 'Log in'}
                    </button>
                    {error && <p className="error">{error}</p>}
                    <p className="hint">
                        {signingUp ? 'Already have an account? ' : 'New here? '}
                        <button type="button" className="link" onClick={() => switchTo(signingUp ? 'login' : 'signup')}>
                            {signingUp ? 'Log in' : 'Sign up'}
                        </button>
                        {' · '}
                        <button type="button" className="link" onClick={() => switchTo('choose')}>
                            Back
                        </button>
                    </p>
                </form>
            )}
        </div>
    )
}

function errorMessage(e: unknown): string {
    if (e instanceof ApiError) {
        return e.problems.length > 0 ? e.problems.join(' ') : e.message
    }

    return e instanceof Error ? e.message : String(e)
}