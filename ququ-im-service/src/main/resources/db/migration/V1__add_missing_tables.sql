CREATE TABLE IF NOT EXISTS im_group_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'group message id',
  message_id BIGINT NOT NULL COMMENT 'related im_message id',
  conv_id BIGINT NOT NULL COMMENT 'conversation id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  sender_id BIGINT NOT NULL COMMENT 'sender user id',
  msg_type TINYINT NOT NULL DEFAULT 1 COMMENT 'message type',
  content VARCHAR(2000) NOT NULL COMMENT 'message content',
  mention_all TINYINT NOT NULL DEFAULT 0 COMMENT 'whether mention all members',
  mention_user_ids JSON DEFAULT NULL COMMENT 'mentioned user ids',
  related_type VARCHAR(50) DEFAULT NULL COMMENT 'related business type',
  related_id BIGINT DEFAULT NULL COMMENT 'related business id',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 normal, 2 recalled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_im_group_message_message (message_id),
  KEY idx_im_group_message_team_created (team_id, created_at, id),
  KEY idx_im_group_message_sender_created (sender_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM group message extension table';
