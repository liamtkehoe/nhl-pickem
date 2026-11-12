import { useEffect, useState } from 'react'
import type { Award, OverUnder } from '../api'

export interface Draft {
    order: Record<string, string[]>
    overUnders: Record<string, OverUnder>
    awards: Partial<Record<Award, number | string>>
}

const EMPTY: Draft = { order: {}, overUnders: {}, awards: {} }

export function useDraft(username: string) {
    const key = `pickem-draft-${username.toLowerCase()}`

    const [draft, setDraft] = useState<Draft>(() => {
        try {
            const saved = localStorage.getItem(key)
            return saved ? (JSON.parse(saved) as Draft) : EMPTY
        } catch {
            return EMPTY
        }
    })

    //Save on every change
    useEffect(() => {
        try {
            localStorage.setItem(key, JSON.stringify(draft))
        } catch {

        }
    }, [key, draft])

    function clear() {
        try {
            localStorage.removeItem(key)
        } catch {

        }
    }

    return { draft, setDraft, clear }
}