import type { LeaderboardEntry } from '../api'
import { PlaceTrophy } from './PlaceTrophy'
import { formatDate } from './shared'

interface Props {
    entries: LeaderboardEntry[]
    me: string
    showTrophies?: boolean
    onSelect: (username: string) => void
}

export function Leaderboard({ entries, me, showTrophies = false, onSelect }: Props) {
    if (entries.length === 0) return <p>No one has submitted picks yet.</p>

    return (
        <>
            <div className="scroll">
                <table className="table">
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Player</th>
                        <th className="num">Total</th>
                        <th className="num">Standings</th>
                        <th className="num">O/U</th>
                        <th className="num">Awards</th>
                        <th>Submitted</th>
                    </tr>
                    </thead>
                    <tbody>
                    {entries.map(e => (
                        <tr key={e.username} className={e.username.toLowerCase() === me.toLowerCase() ? 'me' : ''}>
                            <td>{e.rank}</td>
                            <td>
                                <button type="button" className="link" onClick={() => onSelect(e.username)}>
                                    {e.username}
                                </button>
                                {showTrophies && <PlaceTrophy rank={e.rank} />}
                            </td>
                            <td className="num">{e.total}</td>
                            <td className="num">{e.standingsPoints}</td>
                            <td className="num">{e.overUnderPoints}</td>
                            <td className="num">{e.awardPoints}</td>
                            <td>{formatDate(e.submittedAt)}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
            <p className="hint">Ties are broken by team selections exactly correct, then correct over/unders.</p>
        </>
    )
}