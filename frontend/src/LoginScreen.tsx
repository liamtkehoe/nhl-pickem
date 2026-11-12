import { useState } from 'react'
import { api, ApiError, type LoginResponse } from './api'

interface Props {
    onLogin: (user: LoginResponse) => void
}

export function LoginScreen({ onLogin }: Props) {
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
            <label htmlFor="username">Username</label>
            <input id="username" value={username} onChange={e => setUsername(e.target.value)} autoFocus />
            <p className="hint">New user? Pick an unused username and it becomes your account. Returning user? Use the same username to log in.</p>
            <button type="submit" className="primary">
                Continue
            </button>
            {error && <p className="error">{error}</p>}
        </form>
    )
}