import type { PickOptions } from '../api'
import { oneDecimal, pace } from './shared'

export function StandingsView({ options }: { options: PickOptions }) {
    return (
        <>
            <div className="division-grid">
                {options.divisions.map(div => (
                    <table key={div.division} className="table">
                        <caption>{div.division}</caption>
                        <thead>
                        <tr>
                            <th>#</th>
                            <th>Team</th>
                            <th className="num">GP</th>
                            <th className="num">Pts</th>
                            <th className="num">Pace</th>
                            <th className="num">Line</th>
                        </tr>
                        </thead>
                        <tbody>
                        {[...div.teams]
                            .sort((a, b) => a.currentPlace - b.currentPlace)
                            .map(t => {
                                const projected = pace(t.points, t.gamesPlayed)
                                const arrow =
                                    projected == null || t.line == null ? '' : projected > t.line ? ' ▲' : ' ▼'
                                return (
                                    <tr key={t.team}>
                                        <td>{t.currentPlace}</td>
                                        <td>{t.teamName}</td>
                                        <td className="num">{t.gamesPlayed}</td>
                                        <td className="num">{t.points}</td>
                                        <td className="num">{oneDecimal(projected)}{arrow}</td>
                                        <td className="num">{t.line ?? '–'}</td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                ))}
            </div>
            <p className="hint">Pace = points per game × 84. ▲ on pace to beat the line, ▼ to fall short.</p>
        </>
    )
}