CREATE TABLE IF NOT EXISTS activity_tag (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'tag id',
  name VARCHAR(50) NOT NULL COMMENT 'tag name',
  category VARCHAR(50) DEFAULT NULL COMMENT 'tag category',
  usage_count INT NOT NULL DEFAULT 0 COMMENT 'usage count',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_tag_name (name),
  KEY idx_activity_tag_category_sort (category, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity tag dictionary';

CREATE TABLE IF NOT EXISTS activity_offline_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'offline record id',
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  admin_id BIGINT NOT NULL COMMENT 'admin id',
  reason VARCHAR(500) NOT NULL COMMENT 'offline reason',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_activity_offline_activity_created (activity_id, created_at),
  KEY idx_activity_offline_admin_created (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity offline operation record';
