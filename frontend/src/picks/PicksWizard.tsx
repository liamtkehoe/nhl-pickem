import { useEffect, useState } from 'react'
import { api, type PickOptions } from '../api'
import { AwardsStep } from './AwardsStep'
import { DivisionStep } from './DivisionStep'
import { ReviewStep } from './ReviewStep'
import { useDraft } from './useDraft'

interface Props {
    username: string
    onSubmitted: () => void
}

export function PicksWizard({ username, onSubmitted }: Props) {
    const [options, setOptions] = useState<PickOptions | null>(null)
    const [loadError, setLoadError] = useState<string | null>(null)
    const [step, setStep] = useState(0)
    const { draft, setDraft, clear } = useDraft(username)

    useEffect(() => {
        api.pickOptions()
            .then(setOptions)
            .catch((e: Error) => setLoadError(e.message))
    }, [])
    
    useEffect(() => {
        if (!options) return
        setDraft(d => {
            const order = { ...d.order }
            let changed = false
            for (const div of options.divisions) {
                if (!order[div.division]) {
                    order[div.division] = div.teams.map(t => t.team).sort()
                    changed = true
                }
            }
            return changed ? { ...d, order } : d
        })
    }, [options, setDraft])

    if (loadError) return <p className="error">Couldn't load the pick screens: {loadError}</p>
    if (!options) return <p>Loading…</p>
    if (!options.open) return <p>Picks are closed for this season.</p>
    if (options.divisions.length === 0) {
        return <p>No standings yet. Make sure the backend is running and has synced, then reload.</p>
    }

    const divisions = options.divisions
    const stepNames = [...divisions.map(d => d.division), 'Awards', 'Review']
    const last = stepNames.length - 1

    return (
        <div className="wizard">
            <ol className="steps">
                {stepNames.map((name, i) => (
                    <li key={name} className={i === step ? 'current' : i < step ? 'done' : ''}>
                        <button type="button" onClick={() => setStep(i)}>
                            {name}
                        </button>
                    </li>
                ))}
            </ol>

            {step < divisions.length && (
                <DivisionStep division={divisions[step]} draft={draft} setDraft={setDraft} />
            )}
            {step === divisions.length && <AwardsStep options={options} draft={draft} setDraft={setDraft} />}
            {step === last && (
                <ReviewStep
                    options={options}
                    draft={draft}
                    onSubmitted={() => {
                        clear()
                        onSubmitted()
                    }}
                />
            )}

            <div className="nav">
                <button type="button" disabled={step === 0} onClick={() => setStep(step - 1)}>
                    Back
                </button>
                {step < last && (
                    <button type="button" className="primary" onClick={() => setStep(step + 1)}>
                        Next
                    </button>
                )}
            </div>
        </div>
    )
}