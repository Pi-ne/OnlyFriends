USE ququ_social;

CREATE TABLE IF NOT EXISTS `team_admin_operation_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'operation log id',
  `team_id`        BIGINT       NOT NULL COMMENT 'team id',
  `admin_id`       BIGINT       NOT NULL COMMENT 'admin id',
  `operation_type` VARCHAR(50)  NOT NULL COMMENT 'operation type',
  `reason`         VARCHAR(500) DEFAULT NULL COMMENT 'operation reason',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_created` (`team_id`, `created_at`),
  INDEX `idx_admin_created` (`admin_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team admin operation log table';
