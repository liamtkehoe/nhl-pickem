import { useEffect } from 'react'
import type { LeaderboardEntry } from '../api'
import { Confetti } from './Confetti'
import { Leaderboard } from './Leaderboard'
import { PlaceTrophy } from './PlaceTrophy'
import { ordinal } from './shared'

interface Props {
    entries: LeaderboardEntry[]
    me: string
    seasonLabel: string
    celebrate: boolean
    onClose: () => void
    onSelect: (username: string) => void
}

const sameName = (a: string, b: string) => a.toLowerCase() === b.toLowerCase()

export function SeasonSummary({ entries, me, seasonLabel, celebrate, onClose, onSelect }: Props) {
    const mine = entries.find(e => sameName(e.username, me))
    const onPodium = mine !== undefined && mine.rank <= 3

    //Escape closes the summary.
    useEffect(() => {
        function onKey(e: KeyboardEvent) {
            if (e.key === 'Escape') onClose()
        }
        window.addEventListener('keydown', onKey)
        return () => window.removeEventListener('keydown', onKey)
    }, [onClose])

    return (
        <div className="modal-backdrop" onClick={onClose}>
            {celebrate && onPodium && <Confetti />}
            <div
                className="modal season-summary"
                role="dialog"
                aria-modal="true"
                aria-labelledby="season-summary-title"
                onClick={e => e.stopPropagation()}
            >
                <button type="button" className="modal-close" onClick={onClose} aria-label="Close">
                    ×
                </button>

                <p className="muted">{seasonLabel} Season · Final Standings</p>
                <h2 id="season-summary-title">{onPodium ? 'Congratulations!' : 'Better luck next season!'}</h2>
                {mine && (
                    <p>
                        You finished {ordinal(mine.rank)} of {entries.length} with {mine.total} points.
                    </p>
                )}

                <div className="podium">
                    {[2, 1, 3].map(place => {
                        const standing = entries.filter(e => e.rank === place)
                        return (
                            <div key={place} className={`podium-step place-${place}`}>
                                <div className="podium-names">
                                    {standing.length === 0 ? (
                                        <span className="muted">—</span>
                                    ) : (
                                        standing.map(e => (
                                            <span key={e.username} className={sameName(e.username, me) ? 'me' : ''}>
                                                {e.username}
                                                <span className="muted"> · {e.total}</span>
                                            </span>
                                        ))
                                    )}
                                </div>
                                <div className="podium-block">
                                    <PlaceTrophy rank={place} size={32} />
                                    <span>{ordinal(place)}</span>
                                </div>
                            </div>
                        )
                    })}
                </div>

                <Leaderboard entries={entries} me={me} showTrophies onSelect={onSelect} />
            </div>
        </div>
    )
}