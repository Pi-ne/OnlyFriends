CREATE DATABASE IF NOT EXISTS onlyfriends_activity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE onlyfriends_activity;

DROP TABLE IF EXISTS `activity_review_record`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `activity_comment`;
DROP TABLE IF EXISTS `activity_summary`;
DROP TABLE IF EXISTS `activity_checkin`;
DROP TABLE IF EXISTS `activity_offline_record`;
DROP TABLE IF EXISTS `activity_waitlist`;
DROP TABLE IF EXISTS `activity_registration`;
DROP TABLE IF EXISTS `activity`;
DROP TABLE IF EXISTS `activity_tag`;
DROP TABLE IF EXISTS `activity_template`;

CREATE TABLE `activity_template` (
  `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
  `name`                     VARCHAR(100) NOT NULL COMMENT '模板名称',
  `category`                 VARCHAR(50)  NOT NULL COMMENT '分类',
  `description`              TEXT         DEFAULT NULL COMMENT '模板简介',
  `default_tags`             JSON         DEFAULT NULL COMMENT '默认标签',
  `default_duration`         INT          DEFAULT 2 COMMENT '默认时长（小时）',
  `default_max_participants` INT          DEFAULT 20 COMMENT '默认人数上限',
  `safety_notes`             TEXT         DEFAULT NULL COMMENT '安全须知模板',
  `cover_url`                VARCHAR(500) DEFAULT NULL COMMENT '模板封面图',
  `sort_order`               INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
  `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动模板表';

CREATE TABLE `activity_tag` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '标签名称',
  `category`    VARCHAR(50) DEFAULT NULL COMMENT '标签分类',
  `usage_count` INT         NOT NULL DEFAULT 0 COMMENT '使用次数',
  `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '排序权重',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_tag_name` (`name`),
  INDEX `idx_activity_tag_category_sort` (`category`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动标签字典表';

CREATE TABLE `activity` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `creator_id`       BIGINT        NOT NULL COMMENT '发起人用户ID',
  `title`            VARCHAR(100)  NOT NULL COMMENT '活动名称',
  `description`      TEXT          DEFAULT NULL COMMENT '活动简介',
  `tags`             JSON          DEFAULT NULL COMMENT '活动标签列表',
  `cover_url`        VARCHAR(500)  DEFAULT NULL COMMENT '封面图URL',
  `start_time`       DATETIME      NOT NULL COMMENT '活动开始时间',
  `end_time`         DATETIME      NOT NULL COMMENT '活动结束时间',
  `reg_deadline`     DATETIME      NOT NULL COMMENT '报名截止时间',
  `location_name`    VARCHAR(200)  DEFAULT NULL COMMENT '地点名称',
  `location_lat`     DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `location_lng`     DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `location_detail`  VARCHAR(500)  DEFAULT NULL COMMENT '详细地址',
  `max_participants` INT           NOT NULL DEFAULT 0 COMMENT '人数上限，0表示不限',
  `current_count`    INT           NOT NULL DEFAULT 0 COMMENT '当前报名人数',
  `fee`              DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '活动费用',
  `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '0草稿 1审核中 2已发布 3报名中 4报名截止 5进行中 6已结束 7已下架 8审核驳回',
  `review_type`      TINYINT       NOT NULL DEFAULT 0 COMMENT '0AI自动 1人工审核',
  `is_team_only`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否仅小队',
  `team_id`          BIGINT        DEFAULT NULL COMMENT '关联小队ID',
  `template_id`      BIGINT        DEFAULT NULL COMMENT '模板ID',
  `clone_from_id`    BIGINT        DEFAULT NULL COMMENT '克隆来源ID',
  `checkin_qr_code`  VARCHAR(500)  DEFAULT NULL COMMENT '签到二维码内容',
  `location_verify`  TINYINT       NOT NULL DEFAULT 0 COMMENT '是否位置校验',
  `location_radius`  INT           NOT NULL DEFAULT 500 COMMENT '位置校验半径',
  `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_creator` (`creator_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_start_time` (`start_time`),
  INDEX `idx_location_name` (`location_name`),
  INDEX `idx_location` (`location_lat`, `location_lng`),
  INDEX `idx_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动主表';

CREATE TABLE `activity_review_record` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `activity_id`        BIGINT        NOT NULL COMMENT '活动ID',
  `review_stage`       TINYINT       NOT NULL COMMENT '0AI审核 1人工审核',
  `reviewer_id`        BIGINT        DEFAULT NULL COMMENT '人工审核员ID',
  `ai_result`          VARCHAR(20)   DEFAULT NULL COMMENT 'pass/risk/reject',
  `ai_risk_level`      TINYINT       DEFAULT NULL COMMENT 'AI风险等级',
  `ai_risk_categories` JSON          DEFAULT NULL COMMENT '风险类别',
  `ai_reason`          TEXT          DEFAULT NULL COMMENT 'AI审核说明',
  `ai_confidence`      DECIMAL(4,3)  DEFAULT NULL COMMENT 'AI置信度',
  `final_result`       TINYINT       NOT NULL COMMENT '0通过 1驳回 2要求修改 3转人工',
  `review_comment`     TEXT          DEFAULT NULL COMMENT '审核意见',
  `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_activity` (`activity_id`),
  INDEX `idx_reviewer` (`reviewer_id`),
  INDEX `idx_stage` (`review_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动审核记录表';

CREATE TABLE `activity_offline_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '下架记录ID',
  `activity_id` BIGINT       NOT NULL COMMENT '活动ID',
  `admin_id`    BIGINT       NOT NULL COMMENT '管理员ID',
  `reason`      VARCHAR(500) NOT NULL COMMENT '下架原因',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_activity_offline_activity_created` (`activity_id`, `created_at`),
  INDEX `idx_activity_offline_admin_created` (`admin_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动下架记录表';

CREATE TABLE `activity_registration` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `activity_id`   BIGINT   NOT NULL COMMENT '活动ID',
  `user_id`       BIGINT   NOT NULL COMMENT '报名用户ID',
  `status`        TINYINT  NOT NULL DEFAULT 1 COMMENT '1已报名 2已取消',
  `active_key`    BIGINT   NOT NULL DEFAULT 0 COMMENT '0表示当前有效记录，取消后写入本记录ID',
  `registered_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `cancelled_at`  DATETIME DEFAULT NULL COMMENT '取消时间',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user_active` (`activity_id`, `user_id`, `active_key`),
  INDEX `idx_activity_status` (`activity_id`, `status`),
  INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

CREATE TABLE `activity_checkin` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  `activity_id`  BIGINT        NOT NULL COMMENT '活动ID',
  `user_id`      BIGINT        NOT NULL COMMENT '签到用户ID',
  `checkin_lat`  DECIMAL(10,7) DEFAULT NULL COMMENT '签到纬度',
  `checkin_lng`  DECIMAL(10,7) DEFAULT NULL COMMENT '签到经度',
  `checkin_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_checkin_user` (`activity_id`, `user_id`),
  INDEX `idx_activity_checkin_activity` (`activity_id`, `checkin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动签到表';

CREATE TABLE `activity_summary` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '总结ID',
  `activity_id` BIGINT       NOT NULL COMMENT '活动ID',
  `creator_id`  BIGINT       NOT NULL COMMENT '发起人ID',
  `title`       VARCHAR(100) NOT NULL COMMENT '总结标题',
  `content`     TEXT         NOT NULL COMMENT '总结内容',
  `image_urls`  JSON         DEFAULT NULL COMMENT '总结图片URL列表',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_summary_activity` (`activity_id`),
  INDEX `idx_activity_summary_creator` (`creator_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动总结表';

CREATE TABLE `activity_comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `activity_id` BIGINT       NOT NULL COMMENT '活动ID',
  `user_id`     BIGINT       NOT NULL COMMENT '评价用户ID',
  `rating`      TINYINT      NOT NULL COMMENT '评分：1-5',
  `content`     VARCHAR(500) NOT NULL COMMENT '评价内容',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_comment_user` (`activity_id`, `user_id`),
  INDEX `idx_activity_comment_activity_created` (`activity_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动评价表';

CREATE TABLE `activity_waitlist` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '等待队列ID',
  `activity_id`  BIGINT   NOT NULL COMMENT '活动ID',
  `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
  `queue_no`     INT      NOT NULL COMMENT '等待顺序',
  `status`       TINYINT  NOT NULL DEFAULT 0 COMMENT '0等待中 1待确认 2已取消',
  `active_key`   BIGINT   NOT NULL DEFAULT 0 COMMENT '0表示当前有效记录，结束后写入本记录ID',
  `pending_at`   DATETIME DEFAULT NULL COMMENT '标记待确认时间',
  `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user_wait_active` (`activity_id`, `user_id`, `active_key`),
  UNIQUE KEY `uk_activity_queue_no` (`activity_id`, `queue_no`),
  INDEX `idx_activity_status_queue` (`activity_id`, `status`, `queue_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动等待队列表';

CREATE TABLE `notification` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id`      BIGINT       NOT NULL COMMENT '接收用户ID',
  `type`         VARCHAR(50)  NOT NULL COMMENT '通知类型',
  `title`        VARCHAR(100) NOT NULL COMMENT '标题',
  `content`      VARCHAR(500) NOT NULL COMMENT '内容',
  `related_type` VARCHAR(50)  DEFAULT NULL COMMENT '关联对象类型',
  `related_id`   BIGINT       DEFAULT NULL COMMENT '关联对象ID',
  `read_flag`    TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_read_created` (`user_id`, `read_flag`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

INSERT INTO `activity_template` (`name`, `category`, `description`, `default_tags`, `default_duration`, `default_max_participants`, `safety_notes`, `sort_order`) VALUES
('运动健身', '运动健身', '适合跑步、羽毛球、飞盘等运动活动。', JSON_ARRAY('运动', '健身'), 2, 20, '活动前充分热身，量力而行。', 60),
('户外徒步', '户外徒步', '适合公园徒步、城市轻徒步等户外活动。', JSON_ARRAY('徒步', '户外'), 4, 30, '提前确认路线和天气，建议结伴行动。', 50),
('桌游聚会', '桌游聚会', '适合狼人杀、剧本杀、桌面游戏聚会。', JSON_ARRAY('桌游', '聚会'), 3, 12, '注意场地预约和活动时长。', 40),
('学习交流', '学习交流', '适合读书会、技术分享、语言角。', JSON_ARRAY('学习', '交流'), 2, 25, '提前准备主题材料。', 30),
('公益活动', '公益活动', '适合志愿服务、社区公益活动。', JSON_ARRAY('公益', '志愿'), 3, 50, '遵守组织方安全安排。', 20),
('城市探索', '城市探索', '适合城市漫步、展览打卡、文化探索。', JSON_ARRAY('城市探索', '文化'), 3, 20, '注意交通与集合时间。', 10);
