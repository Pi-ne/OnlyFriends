-- OnlyFriends demo seed data.
-- Run after sql/init-all.sql when you want a predictable demo dataset.

SET NAMES utf8mb4;

USE onlyfriends_user;

INSERT INTO `user`
  (id, email, password_hash, nickname, avatar_url, gender, bio, interest_tags, user_type, status, credit_score)
VALUES
  (10001, 'demo.a@example.com', '$2a$12$2CFDnS0RMrhbjgVhnPOpdOHdp0jaXOImDqEBZG1Ahlolty3pCur2a', 'demoA', 'https://example.com/avatar-a.png', 1, 'Demo activity creator', JSON_ARRAY('hiking', 'boardgame'), 0, 1, 100),
  (10002, 'demo.b@example.com', '$2a$12$2CFDnS0RMrhbjgVhnPOpdOHdp0jaXOImDqEBZG1Ahlolty3pCur2a', 'demoB', 'https://example.com/avatar-b.png', 2, 'Demo participant', JSON_ARRAY('sports', 'social'), 0, 1, 100)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  status = VALUES(status),
  credit_score = VALUES(credit_score);

USE onlyfriends_activity;

INSERT INTO activity_tag (name, category, usage_count, sort_order)
VALUES
  ('运动', '运动健身', 10, 100),
  ('跑步', '运动健身', 8, 90),
  ('户外', '户外徒步', 12, 80),
  ('桌游', '桌游聚会', 6, 70),
  ('聚会', '桌游聚会', 15, 60)
ON DUPLICATE KEY UPDATE
  usage_count = VALUES(usage_count),
  sort_order = VALUES(sort_order);

INSERT INTO activity
  (id, creator_id, title, description, tags, cover_url, start_time, end_time, reg_deadline,
   location_name, location_lat, location_lng, location_detail, max_participants, current_count,
   fee, status, review_type, is_team_only, location_verify, location_radius, deleted)
VALUES
  (20001, 10001, '周末奥森公园晨跑',
   '周六早晨在奥林匹克森林公园慢跑 5 公里，适合初跑者。跑完一起在园区咖啡厅吃早餐。',
   JSON_ARRAY('运动', '跑步', '户外'),
   'https://images.unsplash.com/photo-1476480862126-209bfaa8dcc8?w=800',
   '2026-07-05 07:00:00', '2026-07-05 09:00:00', '2026-07-04 20:00:00',
   '奥林匹克森林公园南园', 40.0028000, 116.3903000, '南门集合，地铁8号线森林公园南门站',
   15, 0, 0.00, 2, 0, 0, 1, 300, 0),
  (20002, 10001, '周五晚狼人杀桌游局',
   '下班后一起玩狼人杀，新手友好，有主持人带局。场地已预定，自带零食饮料更佳。',
   JSON_ARRAY('桌游', '聚会', '室内'),
   'https://images.unsplash.com/photo-1611195974226-ef6e9a2a8c8a?w=800',
   '2026-07-04 19:30:00', '2026-07-04 22:30:00', '2026-07-04 17:00:00',
   '中关村创业大街咖啡吧', 39.9836000, 116.3164000, '海淀区中关村大街11号一层',
   12, 0, 0.00, 2, 0, 0, 0, 200, 0)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  tags = VALUES(tags),
  location_name = VALUES(location_name),
  status = VALUES(status);

USE onlyfriends_social;

INSERT INTO team
  (id, owner_id, name, description, tags, join_type, max_members, member_count, status, score)
VALUES
  (30001, 10001, 'Demo Weekend Team', 'Prepared demo team.', JSON_ARRAY('hiking', 'social'), 0, 20, 2, 1, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = VALUES(status),
  member_count = VALUES(member_count);

INSERT INTO team_member (team_id, user_id, role, status, score)
VALUES
  (30001, 10001, 2, 1, 20),
  (30001, 10002, 0, 1, 10)
ON DUPLICATE KEY UPDATE
  role = VALUES(role),
  status = VALUES(status),
  score = VALUES(score);
