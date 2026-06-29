CREATE TABLE IF NOT EXISTS team_disable_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'disable record id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  admin_id BIGINT NOT NULL COMMENT 'admin id',
  reason VARCHAR(500) NOT NULL COMMENT 'disable reason',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_disable_team_created (team_id, created_at),
  KEY idx_team_disable_admin_created (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team disable operation record';
