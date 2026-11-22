import type { Award } from '../api'

/**
 * Trophy pictures for the awards screen. Each value is either a file in frontend/public/trophies/,
 * written as '/trophies/<file name>', or a full https:// URL. Awards left out show a blank tile.
 */
export const TROPHIES: Partial<Record<Award, string>> = {
    HART: '/trophies/hart.png',
    CALDER: '/trophies/calder.png',
    VEZINA: '/trophies/vezina.png',
    NORRIS: '/trophies/norris.png',
    ART_ROSS: '/trophies/art-ross.png',
    SELKE: '/trophies/selke.png',
    JACK_ADAMS: '/trophies/jack-adams.png',
    ROCKET_RICHARD: '/trophies/rocket-richard.png',
}