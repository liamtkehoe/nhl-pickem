import type { AwardOption, PlayerScore } from '../api'
import { STATUS_LABELS, oneDecimal, type TeamInfo } from './shared'

interface Props {
    score: PlayerScore
    teams: Map<string, TeamInfo>
    awards: AwardOption[]
}

export function ScoreBreakdown({ score, teams, awards }: Props) {
    const awardNames = new Map(awards.map(a => [a.award, a.displayName]))
    const teamName = (code: string) => teams.get(code)?.teamName ?? code
    const divisions = [...new Set([...teams.values()].map(t => t.division))]

    return (
        <div className="breakdown">
            <section>
                <h3>
                    Standings <span className="muted">{score.standingsPoints} pts · {score.exactPlaces} exact</span>
                </h3>
                <div className="division-grid">
                    {divisions.map(division => (
                        <table key={division} className="table">
                            <caption>{division}</caption>
                            <thead>
                            <tr>
                                <th>Pick</th>
                                <th>Team</th>
                                <th className="num">Now</th>
                                <th className="num">Pts</th>
                            </tr>
                            </thead>
                            <tbody>
                            {score.standings
                                .filter(r => teams.get(r.team)?.division === division)
                                .sort((a, b) => a.predictedPlace - b.predictedPlace)
                                .map(r => (
                                    <tr key={r.team}>
                                        <td>{r.predictedPlace}</td>
                                        <td>{teamName(r.team)}</td>
                                        <td className="num">{r.actualPlace ?? '–'}</td>
                                        <td className="num"><Points value={r.points} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ))}
                </div>
            </section>

            <section>
                <h3>
                    Over/under <span className="muted">{score.overUnderPoints} pts · {score.correctOverUnders} correct</span>
                </h3>
                <div className="scroll">
                    <table className="table">
                        <thead>
                        <tr>
                            <th>Team</th>
                            <th>Pick</th>
                            <th className="num">Line</th>
                            <th className="num">Pace</th>
                            <th>Status</th>
                            <th className="num">Pts</th>
                        </tr>
                        </thead>
                        <tbody>
                        {[...score.overUnders]
                            .sort((a, b) => teamName(a.team).localeCompare(teamName(b.team)))
                            .map(r => (
                                <tr key={r.team}>
                                    <td>{teamName(r.team)}</td>
                                    <td>{r.pick === 'OVER' ? 'Over' : 'Under'}</td>
                                    <td className="num">{r.line ?? '–'}</td>
                                    <td className="num">{oneDecimal(r.projectedPoints)}</td>
                                    <td className={`status-${r.status}`}>{STATUS_LABELS[r.status]}</td>
                                    <td className="num"><Points value={r.points} /></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </section>

            <section>
                <h3>
                    Awards <span className="muted">{score.awardPoints} pts</span>
                </h3>
                <div className="scroll">
                    <table className="table">
                        <thead>
                        <tr>
                            <th>Award</th>
                            <th>Pick</th>
                            <th>Leader</th>
                            <th className="num">Pts</th>
                        </tr>
                        </thead>
                        <tbody>
                        {score.awards.map(r => (
                            <tr key={r.award}>
                                <td>{awardNames.get(r.award) ?? r.award}</td>
                                <td>{r.pickName}</td>
                                <td>
                                    {r.leaderName ?? <span className="muted">Pending</span>}
                                    {r.leaderName && <span className="muted"> ({r.isFinal ? 'final' : 'projected'})</span>}
                                </td>
                                <td className="num"><Points value={r.points} /></td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    )
}

function Points({ value }: { value: number }) {
    return <span className={value > 0 ? 'points earned' : 'points'}>{value}</span>
}