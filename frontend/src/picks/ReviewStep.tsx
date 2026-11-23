import { useState } from 'react'
import { api, ApiError, type PickOptions } from '../api'
import type { Draft } from './useDraft'
import { findMissing, toRequest } from './draftRequest'

interface Props {
    options: PickOptions
    draft: Draft
    onSubmitted: () => void
}

export function ReviewStep({ options, draft, onSubmitted }: Props) {
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<ApiError | null>(null)

    const missing = findMissing(options, draft)
    const players = new Map(options.players.map(p => [p.playerId, p.name]))
    const teamNames = new Map(options.divisions.flatMap(d => d.teams).map(t => [t.team, t.teamName]))

    async function submit() {
        if (!window.confirm("Submit your picks? They can't be changed afterwards.")) return
        setSubmitting(true)
        setError(null)
        try {
            await api.submitPicks(toRequest(options, draft))
            onSubmitted()
        } catch (e) {
            setError(e instanceof ApiError ? e : new ApiError(0, String(e)))
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <section>
            <h2>Review and submit</h2>
            <div className="review-grid">
                {options.divisions.map(div => (
                    <div key={div.division}>
                        <h3>{div.division}</h3>
                        <ol>
                            {(draft.order[div.division] ?? []).map(code => (
                                <li key={code}>
                                    {teamNames.get(code)}{' '}
                                    <span className="muted">{draft.overUnders[code] ?? '—'}</span>
                                </li>
                            ))}
                        </ol>
                    </div>
                ))}
            </div>

            <h3>Awards</h3>
            <ul>
                {options.awards.map(a => {
                    const value = draft.awards[a.award]
                    const label =
                        typeof value === 'number' ? players.get(value)
                            : typeof value === 'string' ? teamNames.get(value)
                                : undefined
                    return (
                        <li key={a.award}>
                            {a.displayName}: {label ?? <em>not picked</em>}
                        </li>
                    )
                })}
            </ul>

            {missing.length > 0 && (
                <div className="warning">
                    <p>Still missing:</p>
                    <ul>
                        {missing.map(m => (
                            <li key={m}>{m}</li>
                        ))}
                    </ul>
                </div>
            )}

            {error && (
                <div className="error">
                    <p>{error.message}</p>
                    {error.problems.length > 0 && (
                        <ul>
                            {error.problems.map(p => (
                                <li key={p}>{p}</li>
                            ))}
                        </ul>
                    )}
                </div>
            )}

            <button
                type="button"
                className="primary"
                disabled={missing.length > 0 || submitting}
                onClick={submit}
            >
                {submitting ? 'Submitting…' : 'Submit picks'}
            </button>
        </section>
    )
}