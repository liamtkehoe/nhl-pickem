import type { OverUnder, PickOptions, SavedPicks, SubmitPicksRequest } from '../api'
import type { Draft } from './useDraft'

export function findMissing(options: PickOptions, draft: Draft): string[] {
    const missing: string[] = []
    for (const div of options.divisions) {
        for (const t of div.teams) {
            if (!draft.overUnders[t.team]) missing.push(`Over/under for ${t.teamName}`)
        }
    }
    for (const a of options.awards) {
        if (draft.awards[a.award] === undefined) missing.push(a.displayName)
    }
    return missing
}

export function toRequest(options: PickOptions, draft: Draft): SubmitPicksRequest {
    return {
        standings: options.divisions.flatMap(div =>
            (draft.order[div.division] ?? []).map((team, i) => ({ team, place: i + 1 })),
        ),
        overUnders: Object.entries(draft.overUnders).map(([team, pick]) => ({ team, pick })),
        awards: options.awards.flatMap(a => {
            const value = draft.awards[a.award]
            if (value === undefined) return []
            return [{
                award: a.award,
                playerId: typeof value === 'number' ? value : null,
                team: typeof value === 'string' ? value : null,
            }]
        }),
    }
}

export function draftFromSaved(options: PickOptions, saved: SavedPicks): Draft {
    const placeOf = new Map(saved.standings.map(s => [s.team, s.predictedPlace]))

    const order: Record<string, string[]> = {}
    for (const div of options.divisions) {
        order[div.division] = div.teams
            .map(t => t.team)
            .sort((a, b) => (placeOf.get(a) ?? 99) - (placeOf.get(b) ?? 99))
    }

    const overUnders: Record<string, OverUnder> = {}
    for (const o of saved.overUnders) overUnders[o.team] = o.pick

    const awards: Draft['awards'] = {}
    for (const a of saved.awards) awards[a.award] = a.playerId ?? a.team ?? undefined

    return { order, overUnders, awards }
}