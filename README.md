# NHL Pick'em

A season-long NHL prediction game for friends, ran locally. Players rank every division,
call over/under on each team's points line, and pick the season's award winners. A live
dashboard scores everyone as the season unfolds.

## Scoring

| Pick | Points |
| --- | --- |
| Exact division place | 2 |
| One place off | 1 |
| Correct over/under (scored on pace during the season) | 1 |
| Correct award winner (projected during the season) | 3 |

## Stack

Java 25, Spring Boot 4 and SQLite on the backend; React and TypeScript (Vite) on the
frontend. Standings, rosters and stats come from the NHL's public web and stats APIs.

## Play

Requires Java 25 and Node.js.

```bash
./play.sh            # start the game and open http://localhost:8080
./play.sh --rebuild  # after pulling or changing code
```
Or double-click `NHL Pickem.command` in Finder on macOS.

Each start backs up `data/pickem.db` to `data/backups/` (the 10 newest are kept).

## Develop

- Backend: run `NhlPickemApplication` in IntelliJ (port 8080).
- Frontend: `cd frontend && npm run dev` (port 5173; forwards `/api` to 8080).
- Tests: `./mvnw test` and `cd frontend && npm test`.

## Admin
Most admin tasks live on the in-app Admin page; these commands do the same from Terminal.

| Task | Command |
| --- | --- |
| Pull fresh NHL data | `curl -X POST localhost:8080/api/refresh` |
| Load a season's over/under lines (once) | `curl -X PUT localhost:8080/api/admin/lines -H 'Content-Type: application/json' --data @config/lines-2026-27.json -H 'X-Pickem-User: LiamKehoe'` |
| See an award's top candidates | `curl localhost:8080/api/admin/award-candidates/CALDER -H 'X-Pickem-User: LiamKehoe'` |
| Record a real winner (June) | `curl -X PUT localhost:8080/api/admin/award-winners -H 'Content-Type: application/json' -d '{"award":"HART","playerId":8478402}' -H 'X-Pickem-User: LiamKehoe'` |

Award formula weights live in `config/award-weights.properties`; restart after changing them.

## Starting a new season

1. In `application.properties`: set `pickem.current-season` (e.g. `20272028`) and
   `pickem.current-season-practice=false`.
2. Create `config/lines-2027-28.json` with all 32 preseason lines.
3. Update head coaches in `frontend/src/data/coaches.ts`.
4. If the schedule length changes, update `pickem.games-per-season` and `GAMES_IN_SEASON`
   in `frontend/src/dashboard/shared.ts`.
5. Review `config/award-weights.properties`.
6. `./play.sh --rebuild`, then load the lines file (see Admin).

## Data

Everything players enter lives in `data/pickem.db` (SQLite), which is never committed.

## Layout

- `src/main/java/.../`
    - `config`: Spring configuration and the season seeder
    - `model`: records shared across the app
    - `nhl`: the two NHL API clients
    - `repository`: SQL
    - `scoring`: pure logic (the scoring engine and award formulas)
    - `service`: game rules (validation, submissions, leaders)
    - `web`: REST controllers
- `frontend/src/`
    - `api.ts`: types and calls
    - `picks/`: the pick wizard
    - `dashboard/`: scores, leaderboard and standings