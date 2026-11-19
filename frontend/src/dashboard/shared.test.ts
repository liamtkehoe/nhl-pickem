import { describe, expect, it } from 'vitest'
import { oneDecimal, ordinal, pace } from './shared'

describe('pace', () => {
    it('carries points per game over 84 games', () => {
        expect(pace(14, 11)).toBeCloseTo(106.9, 1)
    })

    it('is null before the first game', () => {
        expect(pace(0, 0)).toBeNull()
    })
})

describe('ordinal', () => {
    it.each([
        [1, '1st'], [2, '2nd'], [3, '3rd'], [4, '4th'],
        [11, '11th'], [12, '12th'], [13, '13th'],
        [21, '21st'], [22, '22nd'], [111, '111th'],
    ])('%i becomes %s', (n, expected) => {
        expect(ordinal(n)).toBe(expected)
    })
})

describe('oneDecimal', () => {
    it('rounds to one place', () => {
        expect(oneDecimal(106.909)).toBe('106.9')
    })

    it('shows a dash when there is no value', () => {
        expect(oneDecimal(null)).toBe('–')
    })
})