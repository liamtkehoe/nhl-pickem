import { useState, type Dispatch, type SetStateAction } from 'react'
import type { Award, AwardOption, PickOptions, PlayerInfo, TeamOption } from '../api'
import { COACHES } from '../data/coaches'
import { Headshot, TeamLogo, TrophyImage } from '../images'
import type { Draft } from './useDraft'

interface Props {
    options: PickOptions
    draft: Draft
    setDraft: Dispatch<SetStateAction<Draft>>
}

export function AwardsStep({ options, draft, setDraft }: Props) {
    const teams = options.divisions
        .flatMap(d => d.teams)
        .sort((a, b) => a.teamName.localeCompare(b.teamName))

    function choose(award: Award, value: number | string) {
        setDraft(d => ({ ...d, awards: { ...d.awards, [award]: value } }))
    }

    return (
        <section>
            <h2>Awards</h2>
            <p className="hint">
                Pick the winner for each NHL award.
            </p>
            {options.awards.map(option => {
                const value = draft.awards[option.award]
                return option.teamPick ? (
                    <TeamPicker
                        key={option.award}
                        option={option}
                        teams={teams}
                        value={typeof value === 'string' ? value : undefined}
                        onChange={team => choose(option.award, team)}
                    />
                ) : (
                    <PlayerPicker
                        key={option.award}
                        option={option}
                        players={options.players}
                        season={options.season}
                        value={typeof value === 'number' ? value : undefined}
                        onChange={playerId => choose(option.award, playerId)}
                    />
                )
            })}
        </section>
    )
}

interface PlayerPickerProps {
    option: AwardOption
    players: PlayerInfo[]
    season: number
    value: number | undefined
    onChange: (playerId: number) => void
}

function PlayerPicker({ option, players, season, value, onChange }: PlayerPickerProps) {
    const [query, setQuery] = useState('')
    const q = query.trim().toLowerCase()
    const matches = players.filter(
        p =>
            option.positions.includes(p.position) &&
            (!option.rookiesOnly || p.rookie) &&
            (p.playerId === value || p.name.toLowerCase().includes(q) || p.team.toLowerCase() === q),
    )
    const selected = players.find(p => p.playerId === value)

    return (
        <div className="award-row">
            <TrophyImage award={option.award} name={option.displayName} />
            <div className="award">
                <label htmlFor={`award-${option.award}`}>{option.displayName}</label>
                <p className="hint award-description">{option.description}</p>
                {option.rookiesOnly && (
                    <p className="hint award-description">Only rookies who have played this season are listed.</p>
                )}
                <input
                    placeholder="Search by name or a team code like TOR"
                    value={query}
                    onChange={e => setQuery(e.target.value)}
                />
                <select
                    id={`award-${option.award}`}
                    value={value ?? ''}
                    onChange={e => onChange(Number(e.target.value))}
                >
                    <option value="" disabled>
                        Choose a player ({matches.length} match)
                    </option>
                    {matches.slice(0, 200).map(p => (
                        <option key={p.playerId} value={p.playerId}>
                            {p.name} · {p.team} · {p.position}
                        </option>
                    ))}
                </select>
            </div>
            {selected ? (
                <Headshot key={selected.playerId} season={season} player={selected} />
            ) : (
                <span className="pick-image placeholder" aria-hidden="true" />
            )}
        </div>
    )
}

interface TeamPickerProps {
    option: AwardOption
    teams: TeamOption[]
    value: string | undefined
    onChange: (team: string) => void
}

function TeamPicker({ option, teams, value, onChange }: TeamPickerProps) {
    return (
        <div className="award-row">
            <TrophyImage award={option.award} name={option.displayName} />
            <div className="award">
                <label htmlFor={`award-${option.award}`}>{option.displayName}</label>
                <p className="hint award-description">{option.description}</p>
                <select id={`award-${option.award}`} value={value ?? ''} onChange={e => onChange(e.target.value)}>
                    <option value="" disabled>
                        Choose a team
                    </option>
                    {teams.map(t => (
                        <option key={t.team} value={t.team}>
                            {t.teamName}
                            {COACHES[t.team] ? ` (${COACHES[t.team]})` : ''}
                        </option>
                    ))}
                </select>
            </div>
            {value ? (
                <TeamLogo key={value} team={value} className="pick-image" />
            ) : (
                <span className="pick-image placeholder" aria-hidden="true" />
            )}
        </div>
    )
}