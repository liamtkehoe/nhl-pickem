import { useEffect, useState } from 'react'
import { api, asApiError, type PickOptions, type SeasonInfo } from '../api'
import { PlayersAdmin } from './PlayersAdmin'
import { SeasonAdmin } from './SeasonAdmin'

type Tab = 'players' | 'season'

interface Props {
    me: string
    season: SeasonInfo | null
    onSeasonChanged: () => void
}

export function AdminPage({ me, season, onSeasonChanged }: Props) {
    const [tab, setTab] = useState<Tab>('players')
    const [options, setOptions] = useState<PickOptions | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        api.pickOptions()
            .then(setOptions)
            .catch(e => setError(asApiError(e).message))
    }, [])

    if (error) return <p className="error">{error}</p>
    if (!options) return <p>Loading…</p>

    const finalized = season?.finalized ?? false

    return (
        <div className="admin">
            <h2>Admin</h2>
            <nav className="tabs">
                {(['players', 'season'] as const).map(t => (
                    <button key={t} type="button" className={tab === t ? 'selected' : ''} onClick={() => setTab(t)}>
                        {t === 'players' ? 'Players' : 'Season'}
                    </button>
                ))}
            </nav>
            {tab === 'players' && <PlayersAdmin me={me} options={options} finalized={finalized} />}
            {tab === 'season' && (
                <SeasonAdmin options={options} finalized={finalized} onSeasonChanged={onSeasonChanged} />
            )}
        </div>
    )
}