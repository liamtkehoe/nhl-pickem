import { useState } from 'react'
import { api, ApiError, type LoginResponse, type SeasonInfo } from './api'

interface Props {
    season: SeasonInfo | null
    onLogin: (user: LoginResponse) => void
}

export function LoginScreen({ season, onLogin }: Props) {
    const [username, setUsername] = useState('')
    const [error, setError] = useState<string | null>(null)

    async function login() {
        setError(null)
        try {
            onLogin(await api.login(username))
        } catch (e) {
            if (e instanceof ApiError && e.problems.length > 0) setError(e.problems.join(' '))
            else setError(e instanceof Error ? e.message : String(e))
        }
    }

    return (
        <form
            className="login"
            onSubmit={e => {
                e.preventDefault()
                void login()
            }}
        >
            <h1>NHL Pick'em</h1>
            {season && (
                <p className="subtitle">
                    {season.label} season{season.practice ? ' (practice)' : ''}
                </p>
            )}
            <label htmlFor="username">Username</label>
            <input id="username" value={username} onChange={e => setUsername(e.target.value)} autoFocus />
            <p className="hint">New here? Pick a username and it becomes your account. Back again? Use the same one.</p>
            <button type="submit" className="primary">
                Continue
            </button>
            {error && <p className="error">{error}</p>}
        </form>
    )
}