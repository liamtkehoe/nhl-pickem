import { useState } from 'react'
import type { Award, PlayerInfo } from './api'
import { TROPHIES } from './data/trophies'
import { useTheme } from './theme'

export function TeamLogo({ team, className = 'team-logo' }: { team: string; className?: string }) {
    const { theme } = useTheme()
    const [failed, setFailed] = useState(false)

    if (failed) return <span className={`${className} placeholder`} aria-hidden="true" />
    return (
        <img
            className={className}
            src={`https://assets.nhle.com/logos/nhl/svg/${team}_${theme}.svg`}
            alt=""
            onError={() => setFailed(true)}
        />
    )
}

export function Headshot({ season, player }: { season: number; player: PlayerInfo }) {
    const [failed, setFailed] = useState(false)

    if (failed) {
        return (
            <span className="pick-image placeholder" title={player.name}>
        {initials(player.name)}
      </span>
        )
    }
    return (
        <img
            className="pick-image headshot"
            src={`https://assets.nhle.com/mugs/nhl/${season}/${player.team}/${player.playerId}.png`}
            alt={player.name}
            onError={() => setFailed(true)}
        />
    )
}

export function TrophyImage({ award, name }: { award: Award; name: string }) {
    const [failed, setFailed] = useState(false)
    const src = TROPHIES[award]

    if (!src || failed) return <span className="trophy placeholder" aria-hidden="true" />
    return <img className="trophy" src={src} alt={name} onError={() => setFailed(true)} />
}

function initials(name: string): string {
    return name
        .split(' ')
        .filter(Boolean)
        .map(part => part[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
}