CREATE TABLE IF NOT EXISTS seasons (
    id              INTEGER PRIMARY KEY,            -- e.g. 20262027
    is_practice     INTEGER NOT NULL DEFAULT 0,     -- 0 or 1
    picks_close_at  TEXT                            -- ISO timestamp; NULL = open
);

CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT NOT NULL UNIQUE COLLATE NOCASE,
    created_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS submissions (
    user_id       INTEGER NOT NULL REFERENCES users(id),
    season_id     INTEGER NOT NULL REFERENCES seasons(id),
    submitted_at  TEXT NOT NULL,
    PRIMARY KEY (user_id, season_id)
    );

CREATE TABLE IF NOT EXISTS standings_picks (
    user_id          INTEGER NOT NULL REFERENCES users(id),
    season_id        INTEGER NOT NULL REFERENCES seasons(id),
    team             TEXT NOT NULL,
    predicted_place  INTEGER NOT NULL CHECK (predicted_place BETWEEN 1 AND 8),
    PRIMARY KEY (user_id, season_id, team)
    );

CREATE TABLE IF NOT EXISTS ou_picks (
    user_id    INTEGER NOT NULL REFERENCES users(id),
    season_id  INTEGER NOT NULL REFERENCES seasons(id),
    team       TEXT NOT NULL,
    pick       TEXT NOT NULL CHECK (pick IN ('OVER', 'UNDER')),
    PRIMARY KEY (user_id, season_id, team)
    );

CREATE TABLE IF NOT EXISTS award_picks (
    user_id    INTEGER NOT NULL REFERENCES users(id),
    season_id  INTEGER NOT NULL REFERENCES seasons(id),
    award      TEXT NOT NULL,
    player_id  INTEGER,                 -- set for player awards
    team       TEXT,                    -- set for Jack Adams
    pick_name  TEXT NOT NULL,           -- name shown at the time of the pick
    PRIMARY KEY (user_id, season_id, award),
    CHECK ((player_id IS NULL) <> (team IS NULL))
    );

CREATE TABLE IF NOT EXISTS ou_lines (
    season_id  INTEGER NOT NULL REFERENCES seasons(id),
    team       TEXT NOT NULL,
    line       REAL NOT NULL,
    source     TEXT NOT NULL,
    as_of      TEXT NOT NULL,
    PRIMARY KEY (season_id, team)
    );

CREATE TABLE IF NOT EXISTS team_snapshot (
    season_id     INTEGER NOT NULL REFERENCES seasons(id),
    team          TEXT NOT NULL,
    team_name     TEXT NOT NULL,
    division      TEXT NOT NULL,
    place         INTEGER NOT NULL,
    points        INTEGER NOT NULL,
    games_played  INTEGER NOT NULL,
    fetched_at    TEXT NOT NULL,
    PRIMARY KEY (season_id, team)
    );

CREATE TABLE IF NOT EXISTS player_snapshot (
    season_id     INTEGER NOT NULL REFERENCES seasons(id),
    player_id     INTEGER NOT NULL,
    name          TEXT NOT NULL,
    team          TEXT NOT NULL,
    position      TEXT NOT NULL,             -- C, L, R, D or G
    is_rookie     INTEGER NOT NULL DEFAULT 0,
    games_played  INTEGER NOT NULL DEFAULT 0,
    stats_json    TEXT NOT NULL DEFAULT '{}',
    fetched_at    TEXT NOT NULL,
    PRIMARY KEY (season_id, player_id)
    );

CREATE TABLE IF NOT EXISTS award_results (
    season_id   INTEGER NOT NULL REFERENCES seasons(id),
    award       TEXT NOT NULL,
    player_id   INTEGER,
    team        TEXT,
    status      TEXT NOT NULL CHECK (status IN ('PROJECTED', 'FINAL')),
    source      TEXT NOT NULL CHECK (source IN ('FORMULA', 'API', 'MANUAL')),
    updated_at  TEXT NOT NULL,
    PRIMARY KEY (season_id, award)
    );