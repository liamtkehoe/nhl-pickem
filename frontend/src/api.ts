export type OverUnder = 'OVER' | 'UNDER'
export type Award = | 'HART' | 'CALDER' | 'VEZINA' | 'NORRIS' | 'ART_ROSS' | 'SELKE' | 'JACK_ADAMS' | 'ROCKET_RICHARD'
export type OuStatus = 'PENDING' | 'LEADING' | 'TRAILING' | 'CLINCHED_WIN' | 'CLINCHED_LOSS' | 'PUSH'

export interface StandingsResult {
    team: string
    predictedPlace: number
    actualPlace: number | null
    points: number
}

export interface OuResult {
    team: string
    pick: OverUnder
    line: number | null
    projectedPoints: number | null
    status: OuStatus
    points: number
}

export interface AwardResult {
    award: Award
    pickName: string
    leaderName: string | null
    isFinal: boolean
    points: number
}

export interface PlayerScore {
    username: string
    total: number
    standingsPoints: number
    overUnderPoints: number
    awardPoints: number
    exactPlaces: number
    correctOverUnders: number
    standings: StandingsResult[]
    overUnders: OuResult[]
    awards: AwardResult[]
}

export interface LeaderboardEntry {
    rank: number
    username: string
    total: number
    standingsPoints: number
    overUnderPoints: number
    awardPoints: number
    exactPlaces: number
    correctOverUnders: number
    submittedAt: string
}

export interface LoginResponse {
    id: number
    username: string
    created: boolean
    submittedAt: string | null
}

export interface TeamOption {
    team: string
    teamName: string
    currentPlace: number
    points: number
    gamesPlayed: number
    line: number | null
}

export interface DivisionOption {
    division: string
    teams: TeamOption[]
}

export interface AwardOption {
    award: Award
    displayName: string
    description: string
    teamPick: boolean
    positions: string[]
    rookiesOnly: boolean
}

export interface SeasonInfo {
    id: number
    label: string
    practice: boolean
    open: boolean
    picksCloseAt: string | null
    finalized: boolean
}

export interface PlayerInfo {
    playerId: number
    name: string
    team: string
    position: string
    rookie: boolean
}

export interface PickOptions {
    season: number
    open: boolean
    divisions: DivisionOption[]
    awards: AwardOption[]
    players: PlayerInfo[]
}

export interface SubmitPicksRequest {
    standings: { team: string; place: number }[]
    overUnders: { team: string; pick: OverUnder }[]
    awards: { award: Award; playerId: number | null; team: string | null }[]
}

//Any non-2XX response. problems holds the validator's list when there is one
export class ApiError extends Error {
    status: number
    problems: string[]

    constructor(status: number, message: string, problems: string[] = []) {
        super(message)
        this.status = status
        this.problems = problems
    }
}

let currentUser: string | null = null

export function setCurrentUser(username: string | null) {
    currentUser = username
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const headers: Record<string, string> = {}
    if (currentUser) headers['X-Pickem-User'] = currentUser
    if (body !== undefined) headers['Content-Type'] = 'application/json'

    const response = await fetch(path, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
    })

    if (!response.ok) {
        //The backend's ProblemDetail { status, title, detail, problems? }
        const problem = await response.json().catch(() => ({}))
        throw new ApiError(
            response.status,
            problem.detail ?? `Request failed (${response.status})`,
            problem.problems ?? [],
        )
    }
    return (await response.json()) as T
}

export const api = {
    login: (username: string) => request<LoginResponse>('POST', '/api/login', { username }),
    pickOptions: () => request<PickOptions>('GET', '/api/pick-options'),
    submitPicks: (picks: SubmitPicksRequest) => request<{ submittedAt: string }>('POST', '/api/picks', picks),
    leaderboard: () => request<LeaderboardEntry[]>('GET', '/api/leaderboard'),
    score: (username: string) => request<PlayerScore>('GET', `/api/scores/${encodeURIComponent(username)}`),
    refresh: () => request<{ teams: number; players: number }>('POST', '/api/admin/refresh'),
    season: () => request<SeasonInfo>('GET', '/api/season'),
    signUp: (username: string) => request<LoginResponse>('POST', '/api/signup', { username }),
}