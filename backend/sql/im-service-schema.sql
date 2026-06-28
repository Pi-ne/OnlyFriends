CREATE DATABASE IF NOT EXISTS ququ_im DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ququ_im;

DROP TABLE IF EXISTS `im_conversation_read`;
DROP TABLE IF EXISTS `im_group_message`;
DROP TABLE IF EXISTS `im_message`;
DROP TABLE IF EXISTS `im_conversation`;

CREATE TABLE `im_conversation` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `conv_type`        TINYINT      NOT NULL COMMENT '1私聊 2群聊',
  `user_id_a`        BIGINT       DEFAULT NULL COMMENT '私聊较小用户ID',
  `user_id_b`        BIGINT       DEFAULT NULL COMMENT '私聊较大用户ID',
  `team_id`          BIGINT       DEFAULT NULL COMMENT '群聊小队ID',
  `last_msg_id`      BIGINT       DEFAULT NULL COMMENT '最后一条消息ID',
  `last_msg_preview` VARCHAR(100) DEFAULT NULL COMMENT '最后消息摘要',
  `last_msg_at`      DATETIME     DEFAULT NULL COMMENT '最后消息时间',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_private_pair` (`conv_type`, `user_id_a`, `user_id_b`),
  UNIQUE KEY `uk_group_team` (`conv_type`, `team_id`),
  INDEX `idx_last_msg_at` (`last_msg_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM会话表';

CREATE TABLE `im_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conv_id`     BIGINT       NOT NULL COMMENT '会话ID',
  `conv_type`   TINYINT      NOT NULL COMMENT '1私聊 2群聊',
  `sender_id`   BIGINT       NOT NULL COMMENT '发送人ID',
  `receiver_id` BIGINT       DEFAULT NULL COMMENT '私聊接收人ID',
  `team_id`     BIGINT       DEFAULT NULL COMMENT '群聊小队ID',
  `msg_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT '1文本，后续扩展图片等',
  `content`     VARCHAR(2000) NOT NULL COMMENT '消息内容',
  `mention_all` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否@所有人',
  `mention_user_ids` JSON    DEFAULT NULL COMMENT '@用户ID列表',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT '关联类型，如announcement/vote',
  `related_id`   BIGINT      DEFAULT NULL COMMENT '关联业务ID',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 2已撤回',
  `recalled_at` DATETIME     DEFAULT NULL COMMENT '撤回时间',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_conv_created` (`conv_id`, `created_at`, `id`),
  INDEX `idx_sender` (`sender_id`),
  INDEX `idx_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM消息表';

CREATE TABLE `im_group_message` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '群消息ID',
  `message_id`       BIGINT        NOT NULL COMMENT '关联统一消息ID',
  `conv_id`          BIGINT        NOT NULL COMMENT '会话ID',
  `team_id`          BIGINT        NOT NULL COMMENT '小队ID',
  `sender_id`        BIGINT        NOT NULL COMMENT '发送人ID',
  `msg_type`         TINYINT       NOT NULL DEFAULT 1 COMMENT '消息类型',
  `content`          VARCHAR(2000) NOT NULL COMMENT '消息内容',
  `mention_all`      TINYINT       NOT NULL DEFAULT 0 COMMENT '是否@所有人',
  `mention_user_ids` JSON          DEFAULT NULL COMMENT '@用户ID列表',
  `related_type`     VARCHAR(50)   DEFAULT NULL COMMENT '关联业务类型',
  `related_id`       BIGINT        DEFAULT NULL COMMENT '关联业务ID',
  `status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '1正常 2已撤回',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_im_group_message_message` (`message_id`),
  INDEX `idx_im_group_message_team_created` (`team_id`, `created_at`, `id`),
  INDEX `idx_im_group_message_sender_created` (`sender_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM群聊消息扩展表';

CREATE TABLE `im_conversation_read` (
  `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '已读记录ID',
  `conv_id`          BIGINT   NOT NULL COMMENT '会话ID',
  `user_id`          BIGINT   NOT NULL COMMENT '用户ID',
  `last_read_msg_id` BIGINT   NOT NULL DEFAULT 0 COMMENT '最后已读消息ID',
  `read_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conv_user` (`conv_id`, `user_id`),
  INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM会话已读游标表';
