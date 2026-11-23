import { describe, expect, it } from 'vitest'
import type { PickOptions, SavedPicks } from '../api'
import { draftFromSaved, toRequest } from './draftRequest'

const options: PickOptions = {
    season: 20262027,
    open: true,
    divisions: [
        {
            division: 'Metropolitan',
            teams: [
                { team: 'NJD', teamName: 'New Jersey Devils', currentPlace: 1, points: 20, gamesPlayed: 12, line: 95.5 },
                { team: 'PHI', teamName: 'Philadelphia Flyers', currentPlace: 2, points: 18, gamesPlayed: 12, line: 93.5 },
                { team: 'NYR', teamName: 'New York Rangers', currentPlace: 3, points: 16, gamesPlayed: 12, line: 86.5 },
            ],
        },
    ],
    awards: [
        { award: 'JACK_ADAMS', displayName: 'Jack Adams Award', description: '', teamPick: true, rookiesOnly: false, positions: [] },
        { award: 'HART', displayName: 'Hart Memorial Trophy', description: '', teamPick: false, rookiesOnly: false, positions: ['C'] },
    ],
    players: [],
}

const saved: SavedPicks = {
    standings: [
        { team: 'PHI', predictedPlace: 1 },
        { team: 'NYR', predictedPlace: 2 },
        { team: 'NJD', predictedPlace: 3 },
    ],
    overUnders: [
        { team: 'PHI', pick: 'OVER' },
        { team: 'NYR', pick: 'UNDER' },
        { team: 'NJD', pick: 'OVER' },
    ],
    awards: [
        { award: 'JACK_ADAMS', playerId: null, team: 'PHI', pickName: 'Philadelphia Flyers' },
        { award: 'HART', playerId: 8478402, team: null, pickName: 'A Player' },
    ],
}

describe('draftFromSaved', () => {
    it('orders each division by the saved places', () => {
        expect(draftFromSaved(options, saved).order.Metropolitan).toEqual(['PHI', 'NYR', 'NJD'])
    })

    it('turns back into the same request it came from', () => {
        const request = toRequest(options, draftFromSaved(options, saved))

        expect(request.standings).toEqual([
            { team: 'PHI', place: 1 },
            { team: 'NYR', place: 2 },
            { team: 'NJD', place: 3 },
        ])
        expect(request.awards).toEqual([
            { award: 'JACK_ADAMS', playerId: null, team: 'PHI' },
            { award: 'HART', playerId: 8478402, team: null },
        ])
    })
})