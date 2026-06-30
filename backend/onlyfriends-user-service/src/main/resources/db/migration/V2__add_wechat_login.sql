-- Idempotent migration: safe when columns/index already exist (e.g. manual SQL was applied first).

SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user' AND COLUMN_NAME = 'email' AND IS_NULLABLE = 'NO') > 0,
    'ALTER TABLE `user` MODIFY COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT ''login email, null for WeChat-only users''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user' AND COLUMN_NAME = 'password_hash' AND IS_NULLABLE = 'NO') > 0,
    'ALTER TABLE `user` MODIFY COLUMN `password_hash` VARCHAR(255) DEFAULT NULL COMMENT ''BCrypt password hash, null for WeChat-only users''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user' AND COLUMN_NAME = 'wx_openid') = 0,
    'ALTER TABLE `user` ADD COLUMN `wx_openid` VARCHAR(64) DEFAULT NULL COMMENT ''WeChat mini program openid'' AFTER `password_hash`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user' AND COLUMN_NAME = 'wx_unionid') = 0,
    'ALTER TABLE `user` ADD COLUMN `wx_unionid` VARCHAR(64) DEFAULT NULL COMMENT ''WeChat unionid'' AFTER `wx_openid`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_wx_openid') = 0,
    'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_wx_openid` (`wx_openid`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
