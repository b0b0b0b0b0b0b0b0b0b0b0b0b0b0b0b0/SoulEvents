CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS airdrop_sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    type_id VARCHAR(64) NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    phase VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    started_at BIGINT NOT NULL,
    ended_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_airdrop_sessions_type ON airdrop_sessions(type_id);

CREATE TABLE IF NOT EXISTS airdrop_spawn_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id VARCHAR(36) NOT NULL,
    type_id VARCHAR(64) NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    source VARCHAR(32) NOT NULL,
    spawned_at BIGINT NOT NULL
);
