import type { OuStatus, PickOptions, TeamOption } from '../api'

//Keep equal to pickem.games-per-season on the backend
export const GAMES_IN_SEASON = 84

export interface TeamInfo extends TeamOption {
    division: string
}

export function teamsByCode(options: PickOptions): Map<string, TeamInfo> {
    return new Map(
        options.divisions.flatMap(d =>
            d.teams.map(t => [t.team, { ...t, division: d.division }] as const),
        ),
    )
}

export function pace(points: number, gamesPlayed: number): number | null {
    return gamesPlayed > 0 ? (points / gamesPlayed) * GAMES_IN_SEASON : null
}

export function oneDecimal(value: number | null | undefined): string {
    return value == null ? '–' : value.toFixed(1)
}

export function ordinal(n: number): string {
    const lastTwo = n % 100
    if (lastTwo >= 11 && lastTwo <= 13) return `${n}th`
    const suffix = ['th', 'st', 'nd', 'rd'][n % 10] ?? 'th'
    return `${n}${suffix}`
}

export function formatDate(iso: string | null | undefined): string {
    if (!iso) return '–'
    return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

export const STATUS_LABELS: Record<OuStatus, string> = {
    PENDING: 'Pending',
    LEADING: 'On pace',
    TRAILING: 'Off pace',
    CLINCHED_WIN: 'Clinched ✓',
    CLINCHED_LOSS: 'Lost ✗',
    PUSH: 'Push',
}