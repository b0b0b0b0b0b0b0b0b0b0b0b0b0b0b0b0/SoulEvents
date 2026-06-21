-- idempotent
CREATE TABLE IF NOT EXISTS player_stats (
    player_uuid TEXT NOT NULL,
    module_id TEXT NOT NULL,
    scope_id TEXT NOT NULL,
    metric TEXT NOT NULL,
    value INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (player_uuid, module_id, scope_id, metric)
);

CREATE INDEX IF NOT EXISTS idx_player_stats_player ON player_stats(player_uuid);
CREATE INDEX IF NOT EXISTS idx_player_stats_module_metric ON player_stats(module_id, metric);
