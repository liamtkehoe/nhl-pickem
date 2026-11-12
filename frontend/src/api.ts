export type OverUnder = 'OVER' | 'UNDER'
export type Award = | 'HART' | 'CALDER' | 'VEZINA' | 'NORRIS' | 'ART_ROSS' | 'SELKE' | 'JACK_ADAMS' | 'ROCKET_RICHARD'

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
    teamPick: boolean
    positions: string[]
}

export interface PlayerInfo {
    playerId: number
    name: string
    team: string
    position: string
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
    submitPicks: (picks: SubmitPicksRequest) =>
        request<{ submittedAt: string }>('POST', '/api/picks', picks),
}