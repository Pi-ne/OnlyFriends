CREATE DATABASE IF NOT EXISTS onlyfriends_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE onlyfriends_user;

DROP TABLE IF EXISTS `merchant_apply`;
DROP TABLE IF EXISTS `merchant_info`;
DROP TABLE IF EXISTS `user_ban_record`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email`          VARCHAR(100)  DEFAULT NULL COMMENT '邮箱（邮箱注册用户，可为空）',
  `password_hash`  VARCHAR(255)  DEFAULT NULL COMMENT '密码哈希（BCrypt，微信用户可为空）',
  `wx_openid`      VARCHAR(64)   DEFAULT NULL COMMENT '微信小程序 openid',
  `wx_unionid`     VARCHAR(64)   DEFAULT NULL COMMENT '微信 unionid',
  `nickname`       VARCHAR(50)   NOT NULL COMMENT '昵称（全平台唯一）',
  `avatar_url`     VARCHAR(500)  DEFAULT NULL COMMENT '头像URL',
  `gender`         TINYINT       DEFAULT 0 COMMENT '性别：0未知 1男 2女',
  `birthday`       DATE          DEFAULT NULL COMMENT '生日',
  `bio`            VARCHAR(200)  DEFAULT NULL COMMENT '个性签名',
  `interest_tags`  JSON          DEFAULT NULL COMMENT '兴趣标签列表',
  `user_type`      TINYINT       NOT NULL DEFAULT 0 COMMENT '用户类型：0个人 1商家',
  `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '账号状态：0未激活 1正常 2封禁',
  `credit_score`   INT           NOT NULL DEFAULT 100 COMMENT '信誉分（0-100）',
  `activate_token` VARCHAR(100)  DEFAULT NULL COMMENT '邮箱激活Token',
  `ban_expire_at`  DATETIME      DEFAULT NULL COMMENT '封禁到期时间（NULL=永久封禁）',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_wx_openid` (`wx_openid`),
  UNIQUE KEY `uk_nickname` (`nickname`),
  INDEX `idx_status` (`status`),
  INDEX `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `merchant_info` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT       NOT NULL COMMENT '关联用户ID',
  `merchant_name`   VARCHAR(100) NOT NULL COMMENT '商家名称',
  `merchant_nick`   VARCHAR(50)  DEFAULT NULL COMMENT '商家昵称',
  `focus_tags`      JSON         DEFAULT NULL COMMENT '商家关注活动领域标签',
  `license_url`     VARCHAR(500) DEFAULT NULL COMMENT '营业执照/凭证URL',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家信息表';

CREATE TABLE `merchant_apply` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL COMMENT '申请人用户ID',
  `merchant_name` VARCHAR(100) NOT NULL COMMENT '申请商家名称',
  `license_url`   VARCHAR(500) NOT NULL COMMENT '上传的营业执照URL',
  `focus_tags`    JSON         DEFAULT NULL COMMENT '商家关注活动领域标签',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1通过 2驳回',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
  `reviewer_id`   BIGINT       DEFAULT NULL COMMENT '审核管理员ID',
  `reviewed_at`   DATETIME     DEFAULT NULL COMMENT '审核时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家申请表';

CREATE TABLE `user_ban_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
  `admin_id`      BIGINT       NOT NULL COMMENT '操作管理员ID',
  `reason`        VARCHAR(500) NOT NULL COMMENT '封禁原因',
  `ban_expire_at` DATETIME     DEFAULT NULL COMMENT '封禁到期时间（NULL=永久）',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_admin_id` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户封禁记录表';
