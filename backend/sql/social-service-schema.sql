CREATE DATABASE IF NOT EXISTS ququ_social DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ququ_social;

DROP TABLE IF EXISTS `team_vote_record`;
DROP TABLE IF EXISTS `team_vote_option`;
DROP TABLE IF EXISTS `team_vote`;
DROP TABLE IF EXISTS `team_score_log`;
DROP TABLE IF EXISTS `team_file`;
DROP TABLE IF EXISTS `team_album`;
DROP TABLE IF EXISTS `team_announcement`;
DROP TABLE IF EXISTS `team_disable_record`;
DROP TABLE IF EXISTS `team_admin_operation_log`;
DROP TABLE IF EXISTS `team_join_apply`;
DROP TABLE IF EXISTS `team_member`;
DROP TABLE IF EXISTS `team`;
DROP TABLE IF EXISTS `friend_apply`;
DROP TABLE IF EXISTS `friend_relation`;
DROP TABLE IF EXISTS `user_follow`;

CREATE TABLE `user_follow` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `follower_id`   BIGINT   NOT NULL COMMENT '关注者用户ID',
  `following_id`  BIGINT   NOT NULL COMMENT '被关注用户ID',
  `status`        TINYINT  NOT NULL DEFAULT 1 COMMENT '1有效',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
  INDEX `idx_follower` (`follower_id`),
  INDEX `idx_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';

CREATE TABLE `friend_relation` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '好友关系ID',
  `user_id_a`   BIGINT   NOT NULL COMMENT '较小用户ID',
  `user_id_b`   BIGINT   NOT NULL COMMENT '较大用户ID',
  `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '1好友',
  `remark_a`    VARCHAR(50) DEFAULT NULL COMMENT '用户A对用户B的备注',
  `remark_b`    VARCHAR(50) DEFAULT NULL COMMENT '用户B对用户A的备注',
  `group_a`     VARCHAR(50) DEFAULT NULL COMMENT '用户A侧好友分组',
  `group_b`     VARCHAR(50) DEFAULT NULL COMMENT '用户B侧好友分组',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friend_pair` (`user_id_a`, `user_id_b`),
  INDEX `idx_user_a` (`user_id_a`),
  INDEX `idx_user_b` (`user_id_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

CREATE TABLE `friend_apply` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '好友申请ID',
  `applicant_id` BIGINT       NOT NULL COMMENT '申请人ID',
  `target_id`    BIGINT       NOT NULL COMMENT '接收人ID',
  `message`      VARCHAR(200) DEFAULT NULL COMMENT '申请消息',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1同意 2拒绝',
  `reviewed_at`  DATETIME     DEFAULT NULL COMMENT '处理时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_target_status` (`target_id`, `status`),
  INDEX `idx_applicant_status` (`applicant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

CREATE TABLE `team` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '小队ID',
  `owner_id`     BIGINT       NOT NULL COMMENT '队长用户ID',
  `name`         VARCHAR(100) NOT NULL COMMENT '小队名称',
  `description`  VARCHAR(500) DEFAULT NULL COMMENT '小队介绍',
  `tags`         JSON         DEFAULT NULL COMMENT '小队标签',
  `join_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT '0公开加入 1审核加入',
  `max_members`  INT          NOT NULL DEFAULT 100 COMMENT '成员上限',
  `member_count` INT          NOT NULL DEFAULT 0 COMMENT '成员数',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 2已解散',
  `score`        INT          NOT NULL DEFAULT 0 COMMENT '积分榜预留字段',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_owner` (`owner_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兴趣小队表';

CREATE TABLE `team_member` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  `team_id`     BIGINT   NOT NULL COMMENT '小队ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `role`        TINYINT  NOT NULL DEFAULT 0 COMMENT '0普通成员 1管理员预留 2队长',
  `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '1有效',
  `score`       INT      NOT NULL DEFAULT 0 COMMENT '成员积分预留字段',
  `joined_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
  INDEX `idx_user` (`user_id`),
  INDEX `idx_team_status` (`team_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队成员表';

CREATE TABLE `team_join_apply` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '加入申请ID',
  `team_id`       BIGINT       NOT NULL COMMENT '小队ID',
  `user_id`       BIGINT       NOT NULL COMMENT '申请用户ID',
  `message`       VARCHAR(200) DEFAULT NULL COMMENT '申请消息',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1同意 2拒绝',
  `reviewer_id`   BIGINT       DEFAULT NULL COMMENT '审核人ID',
  `reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
  `reviewed_at`   DATETIME     DEFAULT NULL COMMENT '审核时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_status` (`team_id`, `status`),
  INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队加入申请表';

CREATE TABLE `team_announcement` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `team_id`      BIGINT        NOT NULL COMMENT '小队ID',
  `publisher_id` BIGINT        NOT NULL COMMENT '发布人用户ID',
  `title`        VARCHAR(100)  NOT NULL COMMENT '公告标题',
  `content`      VARCHAR(1000) NOT NULL COMMENT '公告内容',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_announcement_team_created` (`team_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队公告表';

CREATE TABLE `team_album` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '相册图片ID',
  `team_id`     BIGINT       NOT NULL COMMENT '小队ID',
  `user_id`     BIGINT       NOT NULL COMMENT '上传用户ID',
  `image_url`   VARCHAR(500) NOT NULL COMMENT '图片地址',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '图片描述',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_album_team_created` (`team_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队相册表';

CREATE TABLE `team_file` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `team_id`    BIGINT       NOT NULL COMMENT '小队ID',
  `user_id`    BIGINT       NOT NULL COMMENT '上传用户ID',
  `file_name`  VARCHAR(200) NOT NULL COMMENT '文件名',
  `file_url`   VARCHAR(500) NOT NULL COMMENT '文件地址',
  `file_size`  BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_file_team_created` (`team_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队文件表';

CREATE TABLE `team_score_log` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '积分日志ID',
  `team_id`      BIGINT      NOT NULL COMMENT '小队ID',
  `user_id`      BIGINT      NOT NULL COMMENT '用户ID',
  `score_change` INT         NOT NULL COMMENT '积分变化',
  `action_type`  VARCHAR(50) NOT NULL COMMENT '积分动作',
  `related_id`   BIGINT      DEFAULT NULL COMMENT '关联业务ID',
  `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_score_log_team_created` (`team_id`, `created_at`),
  INDEX `idx_team_score_log_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队积分日志表';

CREATE TABLE `team_vote` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '投票ID',
  `team_id`     BIGINT       NOT NULL COMMENT '小队ID',
  `creator_id`  BIGINT       NOT NULL COMMENT '创建人用户ID',
  `title`       VARCHAR(100) NOT NULL COMMENT '投票标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '投票描述',
  `multiple`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否多选',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1进行中 2关闭',
  `deadline`    DATETIME     DEFAULT NULL COMMENT '截止时间',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_vote_team_created` (`team_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队投票表';

CREATE TABLE `team_vote_option` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '投票选项ID',
  `vote_id`    BIGINT       NOT NULL COMMENT '投票ID',
  `content`    VARCHAR(100) NOT NULL COMMENT '选项内容',
  `vote_count` INT          NOT NULL DEFAULT 0 COMMENT '得票数',
  `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  INDEX `idx_team_vote_option_vote_sort` (`vote_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队投票选项表';

CREATE TABLE `team_vote_record` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '投票记录ID',
  `vote_id`    BIGINT   NOT NULL COMMENT '投票ID',
  `option_id`  BIGINT   NOT NULL COMMENT '选项ID',
  `user_id`    BIGINT   NOT NULL COMMENT '投票用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_vote_user_option` (`vote_id`, `user_id`, `option_id`),
  INDEX `idx_team_vote_record_vote_user` (`vote_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队投票记录表';

CREATE TABLE `team_disable_record` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '停用记录ID',
  `team_id`    BIGINT       NOT NULL COMMENT '小队ID',
  `admin_id`   BIGINT       NOT NULL COMMENT '管理员ID',
  `reason`     VARCHAR(500) NOT NULL COMMENT '停用原因',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_disable_team_created` (`team_id`, `created_at`),
  INDEX `idx_team_disable_admin_created` (`admin_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队停用记录表';

CREATE TABLE `team_admin_operation_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
  `team_id`        BIGINT       NOT NULL COMMENT '小队ID',
  `admin_id`       BIGINT       NOT NULL COMMENT '管理员ID',
  `operation_type` VARCHAR(50)  NOT NULL COMMENT '操作类型',
  `reason`         VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_team_admin_log_team_created` (`team_id`, `created_at`),
  INDEX `idx_team_admin_log_admin_created` (`admin_id`, `created_at`),
  INDEX `idx_team_admin_log_operation` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小队后台管理操作日志表';
