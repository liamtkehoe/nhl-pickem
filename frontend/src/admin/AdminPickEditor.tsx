import { useEffect, useState } from 'react'
import { adminApi, asApiError, type ApiError, type PickOptions } from '../api'
import { AwardsStep } from '../picks/AwardsStep'
import { DivisionStep } from '../picks/DivisionStep'
import { draftFromSaved, findMissing, toRequest } from '../picks/draftRequest'
import type { Draft } from '../picks/useDraft'

interface Props {
    username: string
    options: PickOptions
    onSaved: (message: string) => void
    onCancel: () => void
}

export function AdminPickEditor({ username, options, onSaved, onCancel }: Props) {
    const [initial, setInitial] = useState<Draft | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        adminApi.picks(username)
            .then(saved => setInitial(draftFromSaved(options, saved)))
            .catch(e => setError(asApiError(e).message))
    }, [username, options])

    if (error) {
        return (
            <>
                <p className="error">{error}</p>
                <button type="button" onClick={onCancel}>Back</button>
            </>
        )
    }
    if (!initial) return <p>Loading {username}'s picks…</p>
    return <EditForm username={username} options={options} initial={initial} onSaved={onSaved} onCancel={onCancel} />
}

function EditForm({ username, options, initial, onSaved, onCancel }: Props & { initial: Draft }) {
    const [draft, setDraft] = useState<Draft>(initial)
    const [step, setStep] = useState(0)
    const [error, setError] = useState<ApiError | null>(null)
    const [saving, setSaving] = useState(false)

    const divisions = options.divisions
    const stepNames = [...divisions.map(d => d.division), 'Awards']
    const missing = findMissing(options, draft)

    async function save() {
        if (!window.confirm(`Replace ${username}'s picks?`)) return
        setSaving(true)
        setError(null)
        try {
            await adminApi.replacePicks(username, toRequest(options, draft))
            onSaved(`Saved ${username}'s picks.`)
        } catch (e) {
            setError(asApiError(e))
        } finally {
            setSaving(false)
        }
    }

    return (
        <section>
            <h3>Editing {username}'s picks</h3>
            <ol className="steps">
                {stepNames.map((name, i) => (
                    <li key={name} className={i === step ? 'current' : ''}>
                        <button type="button" onClick={() => setStep(i)}>
                            {name}
                        </button>
                    </li>
                ))}
            </ol>

            {step < divisions.length ? (
                <DivisionStep division={divisions[step]} draft={draft} setDraft={setDraft} />
            ) : (
                <AwardsStep options={options} draft={draft} setDraft={setDraft} />
            )}

            {missing.length > 0 && <p className="warning">Still missing: {missing.join(', ')}</p>}
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

            <div className="nav">
                <button type="button" onClick={onCancel}>
                    Cancel
                </button>
                <button type="button" className="primary" disabled={saving || missing.length > 0} onClick={save}>
                    {saving ? 'Saving…' : 'Save changes'}
                </button>
            </div>
        </section>
    )
}