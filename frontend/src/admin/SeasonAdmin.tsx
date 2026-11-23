import { useEffect, useState } from 'react'
import { adminApi, asApiError, type ApiError, type Award, type AwardStatus, type PickOptions } from '../api'
import { GAMES_IN_SEASON } from '../dashboard/shared'
import { PlayerPicker, TeamPicker } from '../picks/AwardsStep'

interface Props {
    options: PickOptions
    finalized: boolean
    onSeasonChanged: () => void
}

export function SeasonAdmin({ options, finalized, onSeasonChanged }: Props) {
    const [statuses, setStatuses] = useState<AwardStatus[] | null>(null)
    const [choices, setChoices] = useState<Partial<Record<Award, number | string>>>({})
    const [error, setError] = useState<ApiError | null>(null)
    const [message, setMessage] = useState<string | null>(null)
    const [busy, setBusy] = useState(false)

    useEffect(() => {
        adminApi.awards()
            .then(setStatuses)
            .catch(e => setError(asApiError(e)))
    }, [])

    /** What a picker shows: an unsaved choice, else the recorded winner, else the projected leader. */
    function shown(s: AwardStatus): number | string | undefined {
        return (
            choices[s.award] ??
            s.official?.playerId ?? s.official?.team ??
            s.projected?.playerId ?? s.projected?.team ??
            undefined
        )
    }

    async function run(action: () => Promise<void>, success: string) {
        setBusy(true)
        setError(null)
        setMessage(null)
        try {
            await action()
            setMessage(success)
        } catch (e) {
            setError(asApiError(e))
        } finally {
            setBusy(false)
        }
    }

    function saveWinner(s: AwardStatus) {
        const value = shown(s)
        if (value === undefined) return
        void run(async () => {
            setStatuses(await adminApi.recordWinner(
                s.award,
                typeof value === 'number' ? value : null,
                typeof value === 'string' ? value : null,
            ))
            setChoices(c => ({ ...c, [s.award]: undefined }))
        }, `Saved the ${s.displayName} winner.`)
    }

    function clearWinner(s: AwardStatus) {
        void run(async () => {
            setStatuses(await adminApi.clearWinner(s.award))
            setChoices(c => ({ ...c, [s.award]: undefined }))
        }, `Cleared the ${s.displayName} winner.`)
    }

    function finalize() {
        if (!window.confirm('Finalize the season? Every player will see the season summary, and players, picks and winners lock until you un-finalize.')) return
        void run(async () => {
            await adminApi.finalize()
            onSeasonChanged()
        }, 'Season finalized.')
    }

    function unfinalize() {
        if (!window.confirm('Un-finalize the season? The season summary goes away and changes are allowed again.')) return
        void run(async () => {
            await adminApi.unfinalize()
            onSeasonChanged()
        }, 'Season un-finalized.')
    }

    if (!statuses) return error ? <p className="error">{error.message}</p> : <p>Loading…</p>

    const optionFor = new Map(options.awards.map(a => [a.award, a]))
    const teams = options.divisions
        .flatMap(d => d.teams)
        .sort((a, b) => a.teamName.localeCompare(b.teamName))
    const missing = statuses.filter(s => s.official === null)
    const gamesLeft = options.divisions.some(d => d.teams.some(t => t.gamesPlayed < GAMES_IN_SEASON))

    return (
        <section>
            <div className="admin-finalize">
                <h3>Season status: {finalized ? 'Finalized' : 'In progress'}</h3>
                {finalized ? (
                    <>
                        <p className="hint">Players, picks and winners are locked. Un-finalize to change anything.</p>
                        <button type="button" className="danger" disabled={busy} onClick={unfinalize}>
                            Un-finalize season
                        </button>
                    </>
                ) : (
                    <>
                        <button type="button" className="primary" disabled={busy || missing.length > 0} onClick={finalize}>
                            Finalize season
                        </button>
                        {missing.length > 0 && (
                            <p className="hint">
                                Record a winner for every award first. Still missing: {missing.map(s => s.displayName).join(', ')}.
                            </p>
                        )}
                        {gamesLeft && (
                            <p className="warning">
                                Some teams still have games left. You can finalize anyway, which is handy for testing, but
                                the standings keep moving until the season ends.
                            </p>
                        )}
                    </>
                )}
            </div>

            {message && <p className="notice">{message}</p>}
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

            <h3>Official award winners</h3>
            <fieldset className="admin-fieldset" disabled={finalized || busy}>
                {statuses.map(s => {
                    const option = optionFor.get(s.award)
                    if (!option) return null
                    const value = shown(s)
                    const unsaved = choices[s.award] !== undefined

                    return (
                        <div key={s.award} className="admin-award">
                            <div className="admin-award-status">
                <span>
                  {s.official ? (
                      <>Recorded: <strong>{s.official.name ?? '—'}</strong></>
                  ) : (
                      <span className="warning">Not recorded</span>
                  )}
                    {s.projected && <span className="muted"> · projected leader: {s.projected.name}</span>}
                </span>
                                <span className="actions">
                  {s.official && (
                      <button type="button" onClick={() => clearWinner(s)}>
                          Clear
                      </button>
                  )}
                                    <button
                                        type="button"
                                        className="primary"
                                        disabled={value === undefined || (s.official !== null && !unsaved)}
                                        onClick={() => saveWinner(s)}
                                    >
                    Save winner
                  </button>
                </span>
                            </div>
                            {option.teamPick ? (
                                <TeamPicker
                                    option={option}
                                    teams={teams}
                                    value={typeof value === 'string' ? value : undefined}
                                    onChange={team => setChoices(c => ({ ...c, [s.award]: team }))}
                                />
                            ) : (
                                <PlayerPicker
                                    option={option}
                                    players={options.players}
                                    season={options.season}
                                    value={typeof value === 'number' ? value : undefined}
                                    onChange={id => setChoices(c => ({ ...c, [s.award]: id }))}
                                />
                            )}
                        </div>
                    )
                })}
            </fieldset>
        </section>
    )
}