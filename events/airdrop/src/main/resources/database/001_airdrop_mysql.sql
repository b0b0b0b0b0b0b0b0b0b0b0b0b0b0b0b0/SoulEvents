CREATE TABLE IF NOT EXISTS schema_version (
    version INT NOT NULL
);

CREATE TABLE IF NOT EXISTS airdrop_sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    type_id VARCHAR(64) NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    phase VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    started_at BIGINT NOT NULL,
    ended_at BIGINT
);

CREATE TABLE IF NOT EXISTS airdrop_spawn_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    type_id VARCHAR(64) NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    source VARCHAR(32) NOT NULL,
    spawned_at BIGINT NOT NULL
);
