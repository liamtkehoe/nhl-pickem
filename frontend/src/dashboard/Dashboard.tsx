import { useCallback, useEffect, useState } from 'react'
import { api, type LeaderboardEntry, type PickOptions, type PlayerScore, type SeasonInfo } from '../api'
import { Leaderboard } from './Leaderboard'
import { ScoreBreakdown } from './ScoreBreakdown'
import { SeasonSummary } from './SeasonSummary'
import { StandingsView } from './StandingsView'
import { ordinal, teamsByCode } from './shared'

type Tab = 'mine' | 'leaderboard' | 'standings'

const TAB_LABELS: Record<Tab, string> = {
    mine: 'My picks',
    leaderboard: 'Leaderboard',
    standings: 'Standings',
}

const RELOAD_EVERY_MS = 5 * 60 * 1000

interface Props {
    username: string
    season: SeasonInfo | null
}

export function Dashboard({ username, season }: Props) {
    const [options, setOptions] = useState<PickOptions | null>(null)
    const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([])
    const [myScore, setMyScore] = useState<PlayerScore | null>(null)
    const [tab, setTab] = useState<Tab>('mine')
    const [viewing, setViewing] = useState<PlayerScore | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [refreshing, setRefreshing] = useState(false)
    const [loadedAt, setLoadedAt] = useState<Date | null>(null)
    const [summaryDismissed, setSummaryDismissed] = useState(false)
    const [summaryReopened, setSummaryReopened] = useState(false)

    const load = useCallback(async () => {
        try {
            const [opts, board, mine] = await Promise.all([
                api.pickOptions(),
                api.leaderboard(),
                api.score(username),
            ])
            setOptions(opts)
            setLeaderboard(board)
            setMyScore(mine)
            setLoadedAt(new Date())
            setError(null)
        } catch (e) {
            setError(e instanceof Error ? e.message : String(e))
        }
    }, [username])

    useEffect(() => {
        void load()
        const timer = setInterval(() => void load(), RELOAD_EVERY_MS)
        return () => clearInterval(timer)
    }, [load])

    async function refreshFromNhl() {
        setRefreshing(true)
        try {
            await api.refresh()
            await load()
        } catch (e) {
            setError(e instanceof Error ? e.message : String(e))
        } finally {
            setRefreshing(false)
        }
    }

    async function viewPlayer(name: string) {
        if (name.toLowerCase() === username.toLowerCase()) {
            setViewing(null)
            setTab('mine')
            return
        }
        try {
            setViewing(await api.score(name))
        } catch (e) {
            setError(e instanceof Error ? e.message : String(e))
        }
    }

    function closeSummary() {
        setSummaryDismissed(true)
        setSummaryReopened(false)
    }

    if (!options || !myScore) {
        return error ? <p className="error">{error}</p> : <p>Loading…</p>
    }

    const teams = teamsByCode(options)
    const me = leaderboard.find(e => e.username.toLowerCase() === username.toLowerCase())
    const finalized = season?.finalized ?? false
    const autoSummary = finalized && !summaryDismissed
    const showSummary = finalized && leaderboard.length > 0 && (autoSummary || summaryReopened)

    return (
        <div className="dashboard">
            <section className="summary">
                <div className="total">
                    <span className="big">{myScore.total}</span>
                    <span className="muted">
            points{me ? ` · ${ordinal(me.rank)} of ${leaderboard.length}` : ''}
          </span>
                </div>
                <dl className="split">
                    <div>
                        <dt>Standings</dt>
                        <dd>{myScore.standingsPoints}</dd>
                    </div>
                    <div>
                        <dt>Over/under</dt>
                        <dd>{myScore.overUnderPoints}</dd>
                    </div>
                    <div>
                        <dt>Awards</dt>
                        <dd>{myScore.awardPoints}</dd>
                    </div>
                </dl>
                <div className="refresh">
                    {finalized && (
                        <button type="button" onClick={() => setSummaryReopened(true)}>
                            Season summary
                        </button>
                    )}
                    <button type="button" onClick={refreshFromNhl} disabled={refreshing}>
                        {refreshing ? 'Refreshing…' : 'Refresh from NHL'}
                    </button>
                    {loadedAt && (
                        <span className="muted">
              Updated {loadedAt.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}
            </span>
                    )}
                </div>
            </section>

            {error && <p className="error">{error}</p>}

            <nav className="tabs">
                {(['mine', 'leaderboard', 'standings'] as const).map(t => (
                    <button
                        key={t}
                        type="button"
                        className={tab === t ? 'selected' : ''}
                        onClick={() => {
                            setTab(t)
                            setViewing(null)
                        }}
                    >
                        {TAB_LABELS[t]}
                    </button>
                ))}
            </nav>

            {tab === 'mine' && <ScoreBreakdown score={myScore} teams={teams} awards={options.awards} />}

            {tab === 'leaderboard' && viewing && (
                <>
                    <button type="button" className="link" onClick={() => setViewing(null)}>
                        ← Back to leaderboard
                    </button>
                    <h2>{viewing.username}'s picks</h2>
                    <ScoreBreakdown score={viewing} teams={teams} awards={options.awards} />
                </>
            )}
            {tab === 'leaderboard' && !viewing && (
                <Leaderboard
                    entries={leaderboard}
                    me={username}
                    showTrophies={finalized}
                    onSelect={name => void viewPlayer(name)}
                />
            )}

            {tab === 'standings' && <StandingsView options={options} />}

            {showSummary && (
                <SeasonSummary
                    entries={leaderboard}
                    me={username}
                    seasonLabel={season?.label ?? ''}
                    celebrate={autoSummary}
                    onClose={closeSummary}
                    onSelect={name => {
                        closeSummary()
                        setTab('leaderboard')
                        void viewPlayer(name)
                    }}
                />
            )}
        </div>
    )
}