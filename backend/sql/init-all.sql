-- OnlyFriends local database initialization entry.
-- Target: MySQL 8.x. Run this file with UTF-8/utf8mb4 client settings.
-- This script is intended for a fresh local development database. It drops and
-- recreates the tables used by the currently implemented backend services.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS onlyfriends_user
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS onlyfriends_activity
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS onlyfriends_social
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS onlyfriends_im
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS onlyfriends_admin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE onlyfriends_user;

DROP TABLE IF EXISTS merchant_apply;
DROP TABLE IF EXISTS merchant_info;
DROP TABLE IF EXISTS user_ban_record;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'user id',
  email VARCHAR(100) NOT NULL COMMENT 'login email',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt password hash',
  nickname VARCHAR(50) NOT NULL COMMENT 'unique nickname',
  avatar_url VARCHAR(500) DEFAULT NULL COMMENT 'avatar URL',
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0 unknown, 1 male, 2 female',
  birthday DATE DEFAULT NULL COMMENT 'birthday',
  bio VARCHAR(200) DEFAULT NULL COMMENT 'profile bio',
  interest_tags JSON DEFAULT NULL COMMENT 'interest tags JSON',
  user_type TINYINT NOT NULL DEFAULT 0 COMMENT '0 normal user, 1 merchant',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 inactive, 1 active, 2 banned',
  credit_score INT NOT NULL DEFAULT 100 COMMENT 'credit score',
  activate_token VARCHAR(100) DEFAULT NULL COMMENT 'email activation token',
  ban_expire_at DATETIME DEFAULT NULL COMMENT 'ban expiration, null means permanent',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logical delete flag',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_nickname (nickname),
  KEY idx_user_status (status),
  KEY idx_user_type_status (user_type, status),
  KEY idx_user_created_at (created_at),
  KEY idx_user_deleted_status (deleted, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user account table';

CREATE TABLE merchant_info (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT 'related user id',
  merchant_name VARCHAR(100) NOT NULL COMMENT 'merchant name',
  merchant_nick VARCHAR(50) DEFAULT NULL COMMENT 'merchant display name',
  focus_tags JSON DEFAULT NULL COMMENT 'merchant focus tags JSON',
  license_url VARCHAR(500) DEFAULT NULL COMMENT 'business license URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_merchant_info_user_id (user_id),
  KEY idx_merchant_info_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='merchant profile table';

CREATE TABLE merchant_apply (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT 'applicant user id',
  merchant_name VARCHAR(100) NOT NULL COMMENT 'merchant name',
  license_url VARCHAR(500) NOT NULL COMMENT 'business license URL',
  focus_tags JSON DEFAULT NULL COMMENT 'merchant focus tags JSON',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 approved, 2 rejected',
  reject_reason VARCHAR(500) DEFAULT NULL COMMENT 'reject reason',
  reviewer_id BIGINT DEFAULT NULL COMMENT 'admin reviewer id',
  reviewed_at DATETIME DEFAULT NULL COMMENT 'review time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_merchant_apply_user_id (user_id),
  KEY idx_merchant_apply_status_created (status, created_at),
  KEY idx_merchant_apply_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='merchant application table';

CREATE TABLE user_ban_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT 'banned user id',
  admin_id BIGINT NOT NULL COMMENT 'admin operator id',
  reason VARCHAR(500) NOT NULL COMMENT 'ban reason',
  ban_expire_at DATETIME DEFAULT NULL COMMENT 'ban expiration, null means permanent',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_ban_user_created (user_id, created_at),
  KEY idx_user_ban_admin_created (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user ban record table';

USE onlyfriends_activity;

DROP TABLE IF EXISTS activity_review_record;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS activity_comment;
DROP TABLE IF EXISTS activity_summary;
DROP TABLE IF EXISTS activity_checkin;
DROP TABLE IF EXISTS activity_offline_record;
DROP TABLE IF EXISTS activity_waitlist;
DROP TABLE IF EXISTS activity_registration;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS activity_tag;
DROP TABLE IF EXISTS activity_template;

CREATE TABLE activity_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT 'template name',
  category VARCHAR(50) NOT NULL COMMENT 'category',
  description TEXT DEFAULT NULL COMMENT 'description',
  default_tags JSON DEFAULT NULL COMMENT 'default tags JSON',
  default_duration INT NOT NULL DEFAULT 2 COMMENT 'default duration hours',
  default_max_participants INT NOT NULL DEFAULT 20 COMMENT 'default participant limit',
  safety_notes TEXT DEFAULT NULL COMMENT 'safety notes template',
  cover_url VARCHAR(500) DEFAULT NULL COMMENT 'cover image URL',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_activity_template_category (category),
  KEY idx_activity_template_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity template table';

CREATE TABLE activity_tag (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'tag id',
  name VARCHAR(50) NOT NULL COMMENT 'tag name',
  category VARCHAR(50) DEFAULT NULL COMMENT 'tag category',
  usage_count INT NOT NULL DEFAULT 0 COMMENT 'usage count',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_tag_name (name),
  KEY idx_activity_tag_category_sort (category, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity tag dictionary table';

CREATE TABLE activity (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'activity id',
  creator_id BIGINT NOT NULL COMMENT 'creator user id',
  title VARCHAR(100) NOT NULL COMMENT 'activity title',
  description TEXT DEFAULT NULL COMMENT 'activity description',
  tags JSON DEFAULT NULL COMMENT 'activity tags JSON',
  cover_url VARCHAR(500) DEFAULT NULL COMMENT 'cover image URL',
  start_time DATETIME NOT NULL COMMENT 'start time',
  end_time DATETIME NOT NULL COMMENT 'end time',
  reg_deadline DATETIME NOT NULL COMMENT 'registration deadline',
  location_name VARCHAR(200) DEFAULT NULL COMMENT 'location name',
  location_lat DECIMAL(10,7) DEFAULT NULL COMMENT 'latitude',
  location_lng DECIMAL(10,7) DEFAULT NULL COMMENT 'longitude',
  location_detail VARCHAR(500) DEFAULT NULL COMMENT 'location detail',
  max_participants INT NOT NULL DEFAULT 0 COMMENT '0 means unlimited',
  current_count INT NOT NULL DEFAULT 0 COMMENT 'registered participant count',
  fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'fee',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 draft, 1 reviewing, 2 published, 3 registering, 4 registration closed, 5 ongoing, 6 finished, 7 offline, 8 rejected',
  review_type TINYINT NOT NULL DEFAULT 0 COMMENT '0 AI auto, 1 manual',
  is_team_only TINYINT NOT NULL DEFAULT 0 COMMENT 'team only flag',
  team_id BIGINT DEFAULT NULL COMMENT 'related team id',
  template_id BIGINT DEFAULT NULL COMMENT 'template id',
  clone_from_id BIGINT DEFAULT NULL COMMENT 'cloned activity id',
  checkin_qr_code VARCHAR(500) DEFAULT NULL COMMENT 'check-in QR content',
  location_verify TINYINT NOT NULL DEFAULT 0 COMMENT 'location verify flag',
  location_radius INT NOT NULL DEFAULT 500 COMMENT 'location verify radius meters',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logical delete flag',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_activity_creator_created (creator_id, created_at),
  KEY idx_activity_status_created (status, created_at),
  KEY idx_activity_start_time (start_time),
  KEY idx_activity_location_name (location_name),
  KEY idx_activity_location (location_lat, location_lng),
  KEY idx_activity_team_status (team_id, status),
  KEY idx_activity_deleted_status (deleted, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity table';

CREATE TABLE activity_review_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  review_stage TINYINT NOT NULL COMMENT '0 AI review, 1 manual review',
  reviewer_id BIGINT DEFAULT NULL COMMENT 'admin reviewer id',
  ai_result VARCHAR(20) DEFAULT NULL COMMENT 'pass/risk/reject',
  ai_risk_level TINYINT DEFAULT NULL COMMENT 'AI risk level',
  ai_risk_categories JSON DEFAULT NULL COMMENT 'AI risk categories JSON',
  ai_reason TEXT DEFAULT NULL COMMENT 'AI reason',
  ai_confidence DECIMAL(4,3) DEFAULT NULL COMMENT 'AI confidence',
  final_result TINYINT NOT NULL COMMENT '0 pass, 1 reject, 2 needs modify, 3 manual review',
  review_comment TEXT DEFAULT NULL COMMENT 'review comment',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_review_activity_created (activity_id, created_at),
  KEY idx_review_reviewer_created (reviewer_id, created_at),
  KEY idx_review_stage_created (review_stage, created_at),
  KEY idx_review_ai_result (ai_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity review record table';

CREATE TABLE activity_offline_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'offline record id',
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  admin_id BIGINT NOT NULL COMMENT 'admin id',
  reason VARCHAR(500) NOT NULL COMMENT 'offline reason',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_activity_offline_activity_created (activity_id, created_at),
  KEY idx_activity_offline_admin_created (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity offline record table';

CREATE TABLE activity_registration (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'registration id',
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  user_id BIGINT NOT NULL COMMENT 'user id',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 registered, 2 cancelled',
  active_key BIGINT NOT NULL DEFAULT 0 COMMENT '0 means active record',
  registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'registration time',
  cancelled_at DATETIME DEFAULT NULL COMMENT 'cancel time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_user_active (activity_id, user_id, active_key),
  KEY idx_registration_activity_status (activity_id, status),
  KEY idx_registration_user_status (user_id, status),
  KEY idx_registration_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity registration table';

CREATE TABLE activity_checkin (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'check-in id',
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  user_id BIGINT NOT NULL COMMENT 'user id',
  checkin_lat DECIMAL(10,7) DEFAULT NULL COMMENT 'check-in latitude',
  checkin_lng DECIMAL(10,7) DEFAULT NULL COMMENT 'check-in longitude',
  checkin_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'check-in time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_checkin_user (activity_id, user_id),
  KEY idx_activity_checkin_activity (activity_id, checkin_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity check-in table';

CREATE TABLE activity_summary (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'summary id',
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  creator_id BIGINT NOT NULL COMMENT 'creator user id',
  title VARCHAR(100) NOT NULL COMMENT 'summary title',
  content TEXT NOT NULL COMMENT 'summary content',
  image_urls JSON DEFAULT NULL COMMENT 'summary image URLs JSON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_summary_activity (activity_id),
  KEY idx_activity_summary_creator (creator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity summary table';

CREATE TABLE activity_comment (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'comment id',
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  user_id BIGINT NOT NULL COMMENT 'user id',
  rating TINYINT NOT NULL COMMENT 'rating 1-5',
  content VARCHAR(500) NOT NULL COMMENT 'comment content',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_comment_user (activity_id, user_id),
  KEY idx_activity_comment_activity_created (activity_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity comment table';

CREATE TABLE activity_waitlist (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'waitlist id',
  activity_id BIGINT NOT NULL COMMENT 'activity id',
  user_id BIGINT NOT NULL COMMENT 'user id',
  queue_no INT NOT NULL COMMENT 'queue number',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 waiting, 1 pending confirm, 2 cancelled',
  active_key BIGINT NOT NULL DEFAULT 0 COMMENT '0 means active record',
  pending_at DATETIME DEFAULT NULL COMMENT 'pending confirm time',
  cancelled_at DATETIME DEFAULT NULL COMMENT 'cancel time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_activity_user_wait_active (activity_id, user_id, active_key),
  UNIQUE KEY uk_activity_queue_no (activity_id, queue_no),
  KEY idx_waitlist_activity_status_queue (activity_id, status, queue_no),
  KEY idx_waitlist_user_status (user_id, status),
  KEY idx_waitlist_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='activity waitlist table';

CREATE TABLE notification (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'notification id',
  user_id BIGINT NOT NULL COMMENT 'receiver user id',
  type VARCHAR(50) NOT NULL COMMENT 'notification type',
  title VARCHAR(100) NOT NULL COMMENT 'title',
  content VARCHAR(500) NOT NULL COMMENT 'content',
  related_type VARCHAR(50) DEFAULT NULL COMMENT 'related object type',
  related_id BIGINT DEFAULT NULL COMMENT 'related object id',
  read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '0 unread, 1 read',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notification_user_read_created (user_id, read_flag, created_at),
  KEY idx_notification_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='notification table';

INSERT INTO activity_template
  (name, category, description, default_tags, default_duration, default_max_participants, safety_notes, sort_order)
VALUES
  ('运动健身', '运动健身', '适合跑步、羽毛球、飞盘等运动活动。', JSON_ARRAY('运动', '健身'), 2, 20, '活动前充分热身，量力而行。', 60),
  ('户外徒步', '户外徒步', '适合公园徒步、城市轻徒步等户外活动。', JSON_ARRAY('徒步', '户外'), 4, 30, '提前确认路线和天气，建议结伴行动。', 50),
  ('桌游聚会', '桌游聚会', '适合狼人杀、剧本杀、桌面游戏聚会。', JSON_ARRAY('桌游', '聚会'), 3, 12, '注意场地预约和活动时长。', 40),
  ('学习交流', '学习交流', '适合读书会、技术分享、语言角。', JSON_ARRAY('学习', '交流'), 2, 25, '提前准备主题材料。', 30),
  ('公益活动', '公益活动', '适合志愿服务、社区公益活动。', JSON_ARRAY('公益', '志愿'), 3, 50, '遵守组织方安全安排。', 20),
  ('城市探索', '城市探索', '适合城市漫步、展览打卡、文化探索。', JSON_ARRAY('城市探索', '文化'), 3, 20, '注意交通与集合时间。', 10);

USE onlyfriends_social;

DROP TABLE IF EXISTS team_vote_record;
DROP TABLE IF EXISTS team_vote_option;
DROP TABLE IF EXISTS team_vote;
DROP TABLE IF EXISTS team_score_log;
DROP TABLE IF EXISTS team_file;
DROP TABLE IF EXISTS team_album;
DROP TABLE IF EXISTS team_announcement;
DROP TABLE IF EXISTS team_disable_record;
DROP TABLE IF EXISTS team_admin_operation_log;
DROP TABLE IF EXISTS team_join_apply;
DROP TABLE IF EXISTS team_member;
DROP TABLE IF EXISTS team;
DROP TABLE IF EXISTS friend_apply;
DROP TABLE IF EXISTS friend_relation;
DROP TABLE IF EXISTS user_follow;

CREATE TABLE user_follow (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'follow id',
  follower_id BIGINT NOT NULL COMMENT 'follower user id',
  following_id BIGINT NOT NULL COMMENT 'following user id',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_follow_pair (follower_id, following_id),
  KEY idx_user_follow_follower_status (follower_id, status),
  KEY idx_user_follow_following_status (following_id, status),
  KEY idx_user_follow_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user follow table';

CREATE TABLE friend_relation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'friend relation id',
  user_id_a BIGINT NOT NULL COMMENT 'smaller user id',
  user_id_b BIGINT NOT NULL COMMENT 'larger user id',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 friend',
  remark_a VARCHAR(50) DEFAULT NULL COMMENT 'remark set by user A',
  remark_b VARCHAR(50) DEFAULT NULL COMMENT 'remark set by user B',
  group_a VARCHAR(50) DEFAULT NULL COMMENT 'friend group set by user A',
  group_b VARCHAR(50) DEFAULT NULL COMMENT 'friend group set by user B',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_friend_pair (user_id_a, user_id_b),
  KEY idx_friend_user_a_status (user_id_a, status),
  KEY idx_friend_user_b_status (user_id_b, status),
  KEY idx_friend_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='friend relation table';

CREATE TABLE friend_apply (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'friend application id',
  applicant_id BIGINT NOT NULL COMMENT 'applicant user id',
  target_id BIGINT NOT NULL COMMENT 'target user id',
  message VARCHAR(200) DEFAULT NULL COMMENT 'apply message',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 approved, 2 rejected',
  reviewed_at DATETIME DEFAULT NULL COMMENT 'review time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_friend_apply_target_status (target_id, status),
  KEY idx_friend_apply_applicant_status (applicant_id, status),
  KEY idx_friend_apply_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='friend application table';

CREATE TABLE team (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'team id',
  owner_id BIGINT NOT NULL COMMENT 'owner user id',
  name VARCHAR(100) NOT NULL COMMENT 'team name',
  description VARCHAR(500) DEFAULT NULL COMMENT 'team description',
  tags JSON DEFAULT NULL COMMENT 'team tags JSON',
  join_type TINYINT NOT NULL DEFAULT 1 COMMENT '0 open, 1 approval required',
  max_members INT NOT NULL DEFAULT 100 COMMENT 'member limit',
  member_count INT NOT NULL DEFAULT 0 COMMENT 'member count',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 normal, 2 disabled/dismissed',
  score INT NOT NULL DEFAULT 0 COMMENT 'reserved score',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_owner (owner_id),
  KEY idx_team_status_created (status, created_at),
  KEY idx_team_name (name),
  KEY idx_team_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team table';

CREATE TABLE team_member (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'team member id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  user_id BIGINT NOT NULL COMMENT 'user id',
  role TINYINT NOT NULL DEFAULT 0 COMMENT '0 member, 1 admin, 2 owner',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active',
  score INT NOT NULL DEFAULT 0 COMMENT 'reserved score',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'join time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_team_user (team_id, user_id),
  KEY idx_team_member_user_status (user_id, status),
  KEY idx_team_member_team_status (team_id, status),
  KEY idx_team_member_joined_at (joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team member table';

CREATE TABLE team_join_apply (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'team join application id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  user_id BIGINT NOT NULL COMMENT 'applicant user id',
  message VARCHAR(200) DEFAULT NULL COMMENT 'apply message',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 approved, 2 rejected',
  reviewer_id BIGINT DEFAULT NULL COMMENT 'reviewer user id',
  reject_reason VARCHAR(200) DEFAULT NULL COMMENT 'reject reason',
  reviewed_at DATETIME DEFAULT NULL COMMENT 'review time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_join_apply_team_status (team_id, status),
  KEY idx_team_join_apply_user_status (user_id, status),
  KEY idx_team_join_apply_reviewer (reviewer_id),
  KEY idx_team_join_apply_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team join application table';

CREATE TABLE team_announcement (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'announcement id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  publisher_id BIGINT NOT NULL COMMENT 'publisher user id',
  title VARCHAR(100) NOT NULL COMMENT 'announcement title',
  content VARCHAR(1000) NOT NULL COMMENT 'announcement content',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_announcement_team_created (team_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team announcement table';

CREATE TABLE team_album (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'album image id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  user_id BIGINT NOT NULL COMMENT 'uploader user id',
  image_url VARCHAR(500) NOT NULL COMMENT 'image URL',
  description VARCHAR(200) DEFAULT NULL COMMENT 'image description',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_album_team_created (team_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team album table';

CREATE TABLE team_file (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'file id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  user_id BIGINT NOT NULL COMMENT 'uploader user id',
  file_name VARCHAR(200) NOT NULL COMMENT 'file name',
  file_url VARCHAR(500) NOT NULL COMMENT 'file URL',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT 'file size bytes',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_file_team_created (team_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team file table';

CREATE TABLE team_score_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'score log id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  user_id BIGINT NOT NULL COMMENT 'user id',
  score_change INT NOT NULL COMMENT 'score change',
  action_type VARCHAR(50) NOT NULL COMMENT 'action type',
  related_id BIGINT DEFAULT NULL COMMENT 'related business id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_score_log_team_created (team_id, created_at),
  KEY idx_team_score_log_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team score log table';

CREATE TABLE team_vote (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'vote id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  creator_id BIGINT NOT NULL COMMENT 'creator user id',
  title VARCHAR(100) NOT NULL COMMENT 'vote title',
  description VARCHAR(500) DEFAULT NULL COMMENT 'vote description',
  multiple TINYINT NOT NULL DEFAULT 0 COMMENT 'multiple choice flag',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 open, 2 closed',
  deadline DATETIME DEFAULT NULL COMMENT 'deadline',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_vote_team_created (team_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team vote table';

CREATE TABLE team_vote_option (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'vote option id',
  vote_id BIGINT NOT NULL COMMENT 'vote id',
  content VARCHAR(100) NOT NULL COMMENT 'option content',
  vote_count INT NOT NULL DEFAULT 0 COMMENT 'vote count',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order',
  PRIMARY KEY (id),
  KEY idx_team_vote_option_vote_sort (vote_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team vote option table';

CREATE TABLE team_vote_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'vote record id',
  vote_id BIGINT NOT NULL COMMENT 'vote id',
  option_id BIGINT NOT NULL COMMENT 'option id',
  user_id BIGINT NOT NULL COMMENT 'voter user id',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_team_vote_user_option (vote_id, user_id, option_id),
  KEY idx_team_vote_record_vote_user (vote_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team vote record table';

CREATE TABLE team_disable_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'disable record id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  admin_id BIGINT NOT NULL COMMENT 'admin id',
  reason VARCHAR(500) NOT NULL COMMENT 'disable reason',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_disable_team_created (team_id, created_at),
  KEY idx_team_disable_admin_created (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team disable record table';

CREATE TABLE team_admin_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'operation log id',
  team_id BIGINT NOT NULL COMMENT 'team id',
  admin_id BIGINT NOT NULL COMMENT 'admin id',
  operation_type VARCHAR(50) NOT NULL COMMENT 'operation type',
  reason VARCHAR(500) DEFAULT NULL COMMENT 'operation reason',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_team_admin_log_team_created (team_id, created_at),
  KEY idx_team_admin_log_admin_created (admin_id, created_at),
  KEY idx_team_admin_log_operation (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='team admin operation log table';

USE onlyfriends_im;

DROP TABLE IF EXISTS im_conversation_read;
DROP TABLE IF EXISTS im_group_message;
DROP TABLE IF EXISTS im_message;
DROP TABLE IF EXISTS im_conversation;

CREATE TABLE im_conversation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'conversation id',
  conv_type TINYINT NOT NULL COMMENT '1 private, 2 team group',
  user_id_a BIGINT DEFAULT NULL COMMENT 'private chat smaller user id',
  user_id_b BIGINT DEFAULT NULL COMMENT 'private chat larger user id',
  team_id BIGINT DEFAULT NULL COMMENT 'team id for group chat',
  last_msg_id BIGINT DEFAULT NULL COMMENT 'last message id',
  last_msg_preview VARCHAR(100) DEFAULT NULL COMMENT 'last message preview',
  last_msg_at DATETIME DEFAULT NULL COMMENT 'last message time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_private_pair (conv_type, user_id_a, user_id_b),
  UNIQUE KEY uk_group_team (conv_type, team_id),
  KEY idx_conversation_user_a (user_id_a),
  KEY idx_conversation_user_b (user_id_b),
  KEY idx_conversation_team_id (team_id),
  KEY idx_conversation_last_msg_at (last_msg_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM conversation table';

CREATE TABLE im_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'message id',
  conv_id BIGINT NOT NULL COMMENT 'conversation id',
  conv_type TINYINT NOT NULL COMMENT '1 private, 2 team group',
  sender_id BIGINT NOT NULL COMMENT 'sender user id',
  receiver_id BIGINT DEFAULT NULL COMMENT 'private receiver user id',
  team_id BIGINT DEFAULT NULL COMMENT 'group team id',
  msg_type TINYINT NOT NULL DEFAULT 1 COMMENT '1 text',
  content VARCHAR(2000) NOT NULL COMMENT 'message content',
  mention_all TINYINT NOT NULL DEFAULT 0 COMMENT 'whether mention all members',
  mention_user_ids JSON DEFAULT NULL COMMENT 'mentioned user ids',
  related_type VARCHAR(50) DEFAULT NULL COMMENT 'related business type',
  related_id BIGINT DEFAULT NULL COMMENT 'related business id',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 normal, 2 recalled',
  recalled_at DATETIME DEFAULT NULL COMMENT 'recall time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_message_conv_created (conv_id, created_at, id),
  KEY idx_message_sender_created (sender_id, created_at),
  KEY idx_message_receiver_created (receiver_id, created_at),
  KEY idx_message_team_created (team_id, created_at),
  KEY idx_message_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM message table';

CREATE TABLE im_group_message (
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

CREATE TABLE im_conversation_read (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'read cursor id',
  conv_id BIGINT NOT NULL COMMENT 'conversation id',
  user_id BIGINT NOT NULL COMMENT 'user id',
  last_read_msg_id BIGINT NOT NULL DEFAULT 0 COMMENT 'last read message id',
  read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'read time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_conv_user (conv_id, user_id),
  KEY idx_conversation_read_user (user_id),
  KEY idx_conversation_read_conv (conv_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM conversation read cursor table';

USE onlyfriends_admin;

DROP TABLE IF EXISTS admin_operation_log;
DROP TABLE IF EXISTS admin_user;

CREATE TABLE admin_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'admin id',
  username VARCHAR(50) NOT NULL COMMENT 'login username',
  password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt password hash',
  nickname VARCHAR(50) NOT NULL COMMENT 'admin nickname',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 2 disabled',
  last_login_at DATETIME DEFAULT NULL COMMENT 'last login time',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_username (username),
  KEY idx_admin_status (status),
  KEY idx_admin_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='admin user table';

CREATE TABLE admin_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'operation log id',
  admin_id BIGINT NOT NULL COMMENT 'admin id',
  operation_type VARCHAR(50) NOT NULL COMMENT 'operation type',
  target_type VARCHAR(50) NOT NULL COMMENT 'target type',
  target_id BIGINT DEFAULT NULL COMMENT 'target id',
  detail VARCHAR(500) DEFAULT NULL COMMENT 'operation detail',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_admin_operation_admin_created (admin_id, created_at),
  KEY idx_admin_operation_target (target_type, target_id),
  KEY idx_admin_operation_type_created (operation_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='admin operation log table';

INSERT INTO admin_user (username, password_hash, nickname, status)
VALUES ('admin', '$2a$12$2CFDnS0RMrhbjgVhnPOpdOHdp0jaXOImDqEBZG1Ahlolty3pCur2a', 'System Admin', 1);

SET FOREIGN_KEY_CHECKS = 1;
