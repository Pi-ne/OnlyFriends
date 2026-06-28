CREATE DATABASE IF NOT EXISTS ququ_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ququ_admin;

DROP TABLE IF EXISTS `admin_operation_log`;
DROP TABLE IF EXISTS `admin_user`;

CREATE TABLE `admin_user` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'admin id',
  `username`       VARCHAR(50)  NOT NULL COMMENT 'login username',
  `password_hash`  VARCHAR(255) NOT NULL COMMENT 'bcrypt password hash',
  `nickname`       VARCHAR(50)  NOT NULL COMMENT 'admin nickname',
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1 enabled 2 disabled',
  `last_login_at`  DATETIME     DEFAULT NULL COMMENT 'last login time',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='admin user table';

CREATE TABLE `admin_operation_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'operation log id',
  `admin_id`       BIGINT       NOT NULL COMMENT 'admin id',
  `operation_type` VARCHAR(50)  NOT NULL COMMENT 'operation type',
  `target_type`    VARCHAR(50)  NOT NULL COMMENT 'target type',
  `target_id`      BIGINT       DEFAULT NULL COMMENT 'target id',
  `detail`         VARCHAR(500) DEFAULT NULL COMMENT 'operation detail',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_admin_created` (`admin_id`, `created_at`),
  INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='admin operation log table';

INSERT INTO `admin_user` (`username`, `password_hash`, `nickname`, `status`)
VALUES ('admin', '$2a$12$2CFDnS0RMrhbjgVhnPOpdOHdp0jaXOImDqEBZG1Ahlolty3pCur2a', 'System Admin', 1);
