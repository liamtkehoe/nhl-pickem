const PLACES = {
    1: { tone: 'gold', label: '1st place' },
    2: { tone: 'silver', label: '2nd place' },
    3: { tone: 'bronze', label: '3rd place' },
} as const

export function PlaceTrophy({ rank, size = 16 }: { rank: number; size?: number }) {
    if (rank < 1 || rank > 3) return null
    const { tone, label } = PLACES[rank as 1 | 2 | 3]

    return (
        <svg
            className={`place-trophy ${tone}`}
            viewBox="0 0 24 24"
            width={size}
            height={size}
            role="img"
            aria-label={label}
        >
            <title>{label}</title>
            {/* cup */}
            <path fill="currentColor" d="M7 3h10v6a5 5 0 0 1-10 0V3z" />
            {/* handles */}
            <path
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                d="M7 5H4.5v2.5A3 3 0 0 0 7 10.5M17 5h2.5v2.5A3 3 0 0 1 17 10.5"
            />
            {/* stem and base */}
            <path fill="currentColor" d="M11 14h2v4h-2zM8 18h8v3H8z" />
        </svg>
    )
}