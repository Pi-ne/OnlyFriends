-- Replace smoke-test activity samples with realistic demo data.
SET NAMES utf8mb4;

USE onlyfriends_activity;

UPDATE activity SET
  title = '周末奥森公园晨跑',
  description = '周六早晨在奥林匹克森林公园慢跑 5 公里，适合初跑者。跑完一起在园区咖啡厅吃早餐，欢迎结伴参加。',
  tags = JSON_ARRAY('运动', '跑步', '户外'),
  cover_url = 'https://images.unsplash.com/photo-1476480862126-209bfaa8dcc8?w=800',
  start_time = '2026-07-05 07:00:00',
  end_time = '2026-07-05 09:00:00',
  reg_deadline = '2026-07-04 20:00:00',
  location_name = '奥林匹克森林公园南园',
  location_lat = 40.0028000,
  location_lng = 116.3903000,
  location_detail = '南门集合，地铁8号线森林公园南门站',
  max_participants = 15,
  fee = 0.00,
  status = 2
WHERE id = 1;

UPDATE activity SET
  title = '周五晚狼人杀桌游局',
  description = '下班后一起玩狼人杀，新手友好，有主持人带局。场地已预定，自带零食饮料更佳。',
  tags = JSON_ARRAY('桌游', '聚会', '室内'),
  cover_url = 'https://images.unsplash.com/photo-1611195974226-ef6e9a2a8c8a?w=800',
  start_time = '2026-07-04 19:30:00',
  end_time = '2026-07-04 22:30:00',
  reg_deadline = '2026-07-04 17:00:00',
  location_name = '中关村创业大街咖啡吧',
  location_lat = 39.9836000,
  location_lng = 116.3164000,
  location_detail = '海淀区中关村大街11号一层',
  max_participants = 12,
  fee = 0.00,
  status = 2
WHERE id = 2;

UPDATE activity_comment SET
  content = '氛围很好，组织者很用心，下次还来！'
WHERE activity_id = 1;

UPDATE activity_comment SET
  content = '局很关键，玩得很开心，新手也能很快上手。'
WHERE activity_id = 2;

INSERT INTO activity_tag (name, category, usage_count, sort_order)
VALUES
  ('运动', '运动健身', 5, 100),
  ('跑步', '运动健身', 3, 90),
  ('户外', '户外徒步', 4, 80),
  ('桌游', '桌游聚会', 6, 70),
  ('聚会', '桌游聚会', 8, 60),
  ('室内', '桌游聚会', 2, 50)
ON DUPLICATE KEY UPDATE
  usage_count = VALUES(usage_count),
  sort_order = VALUES(sort_order);
