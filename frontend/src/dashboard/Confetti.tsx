import { useState, type CSSProperties } from 'react'

const COLORS = ['#ffd54a', '#d0d6de', '#d08a4a', '#4ea1ff', '#ff6b8a', '#4cc38a']

interface Piece {
    left: number
    delay: number
    duration: number
    size: number
    spin: number
    color: string
}

function makePieces(count: number): Piece[] {
    return Array.from({ length: count }, (_, i) => ({
        left: Math.random() * 100,
        delay: Math.random() * 1.5,
        duration: 2.5 + Math.random() * 2,
        size: 6 + Math.random() * 6,
        spin: Math.random() * 720 - 360,
        color: COLORS[i % COLORS.length],
    }))
}

export function Confetti({ count = 140 }: { count?: number }) {
    const [pieces] = useState(() => makePieces(count))

    return (
        <div className="confetti" aria-hidden="true">
            {pieces.map((p, i) => (
                <span
                    key={i}
                    style={
                        {
                            left: `${p.left}%`,
                            width: p.size,
                            height: p.size * 0.45,
                            background: p.color,
                            animationDelay: `${p.delay}s`,
                            animationDuration: `${p.duration}s`,
                            '--spin': `${p.spin}deg`,
                        } as CSSProperties
                    }
                />
            ))}
        </div>
    )
}