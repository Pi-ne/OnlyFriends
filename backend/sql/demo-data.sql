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
  ('hiking', 'outdoor', 10, 100),
  ('boardgame', 'indoor', 8, 90),
  ('social', 'common', 20, 80)
ON DUPLICATE KEY UPDATE
  usage_count = VALUES(usage_count),
  sort_order = VALUES(sort_order);

INSERT INTO activity
  (id, creator_id, title, description, tags, cover_url, start_time, end_time, reg_deadline,
   location_name, location_lat, location_lng, location_detail, max_participants, current_count,
   fee, status, review_type, is_team_only, location_verify, location_radius, deleted)
VALUES
  (20001, 10001, 'Demo Weekend Hiking', 'A prepared demo activity.', JSON_ARRAY('hiking', 'social'),
   'https://example.com/demo-hiking.jpg', '2026-07-20 09:00:00', '2026-07-20 12:00:00',
   '2026-07-19 18:00:00', 'Demo Park', 31.2304000, 121.4737000, 'East gate',
   20, 0, 0.00, 2, 0, 0, 0, 500, 0)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
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
