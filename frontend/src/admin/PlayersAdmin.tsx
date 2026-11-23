import { useCallback, useEffect, useState } from 'react'
import { adminApi, asApiError, type AdminUser, type PickOptions } from '../api'
import { formatDate } from '../dashboard/shared'
import { AdminPickEditor } from './AdminPickEditor'

interface Props {
    me: string
    options: PickOptions
    finalized: boolean
}

export function PlayersAdmin({ me, options, finalized }: Props) {
    const [users, setUsers] = useState<AdminUser[] | null>(null)
    const [editing, setEditing] = useState<string | null>(null)
    const [message, setMessage] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)

    const load = useCallback(() => {
        adminApi.users()
            .then(setUsers)
            .catch(e => setError(asApiError(e).message))
    }, [])

    useEffect(() => {
        load()
    }, [load])

    async function remove(username: string) {
        if (!window.confirm(`Delete ${username} and all of their picks? This can't be undone.`)) return
        setMessage(null)
        setError(null)
        try {
            await adminApi.deleteUser(username)
            setMessage(`Deleted ${username}.`)
            load()
        } catch (e) {
            setError(asApiError(e).message)
        }
    }

    if (editing) {
        return (
            <AdminPickEditor
                username={editing}
                options={options}
                onCancel={() => setEditing(null)}
                onSaved={text => {
                    setEditing(null)
                    setMessage(text)
                    load()
                }}
            />
        )
    }

    const isMe = (username: string) => username.toLowerCase() === me.toLowerCase()

    return (
        <section>
            {finalized && (
                <p className="warning">
                    The season is finalized, so players and picks are locked. Un-finalize it on the Season tab to make
                    changes.
                </p>
            )}
            {message && <p className="notice">{message}</p>}
            {error && <p className="error">{error}</p>}

            {users === null ? (
                <p>Loading…</p>
            ) : (
                <div className="scroll">
                    <table className="table">
                        <thead>
                        <tr>
                            <th>Player</th>
                            <th>Joined</th>
                            <th>Submitted</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {users.map(u => (
                            <tr key={u.id}>
                                <td>{u.username}</td>
                                <td>{formatDate(u.createdAt)}</td>
                                <td>{u.submittedAt ? formatDate(u.submittedAt) : <span className="muted">Not yet</span>}</td>
                                <td className="actions">
                                    <button
                                        type="button"
                                        disabled={finalized || !u.submittedAt}
                                        onClick={() => {
                                            setMessage(null)
                                            setError(null)
                                            setEditing(u.username)
                                        }}
                                    >
                                        Edit picks
                                    </button>
                                    <button
                                        type="button"
                                        className="danger"
                                        disabled={finalized || isMe(u.username)}
                                        onClick={() => void remove(u.username)}
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    )
}