# 数据库初始化

本地开发采用**每服务独立数据库**：

- `onlyfriends_user`
- `onlyfriends_activity`
- `onlyfriends_social`
- `onlyfriends_im`
- `onlyfriends_admin`

统一初始化入口：

```text
backend/sql/init-all.sql
```

`sql/` 下仍保留各模块独立脚本，供分步初始化参考。新环境请优先执行 `init-all.sql`，其中包含当前已实现表结构与基线数据。

## 前置条件

先启动 MySQL：

```powershell
cd backend
docker compose up -d mysql
```

`docker-compose.yml` 中默认 root 密码：

```text
onlyfriends_root_password
```

若通过环境变量设置了 `MYSQL_ROOT_PASSWORD`，请改用对应值。

## 执行统一脚本

PowerShell（Docker MySQL 容器）：

```powershell
cd backend
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
```

本地 MySQL 客户端：

```powershell
mysql -h 127.0.0.1 -P 3306 -uroot -p --default-character-set=utf8mb4 < .\sql\init-all.sql
```

该脚本面向**全新本地库**：会删除并重建当前服务表，请勿对需要保留的数据执行。

可选演示数据：

```powershell
Get-Content .\sql\demo-data.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
```

## 基线数据

脚本会插入：

- `onlyfriends_activity.activity_template` 中的活动模板
- `onlyfriends_admin.admin_user` 中的默认管理员

默认管理员登录：

```text
username: admin
password: Admin123456
```

数据库仅存储 BCrypt 密码哈希，不保存明文。

## 表清单

用户库 `onlyfriends_user`：

- `user`
- `merchant_info`
- `merchant_apply`
- `user_ban_record`

活动库 `onlyfriends_activity`：

- `activity_template`、`activity_tag`
- `activity`、`activity_review_record`、`activity_offline_record`
- `activity_registration`、`activity_waitlist`、`activity_checkin`
- `activity_summary`、`activity_comment`
- `notification`

社交库 `onlyfriends_social`：

- `user_follow`、`friend_relation`（含 `remark_a/b`、`group_a/b`）、`friend_apply`
- `team`、`team_member`、`team_join_apply`
- `team_announcement`、`team_album`、`team_file`、`team_score_log`
- `team_vote`、`team_vote_option`、`team_vote_record`
- `team_disable_record`、`team_admin_operation_log`

IM 库 `onlyfriends_im`：

- `im_conversation`、`im_message`、`im_group_message`
- `im_conversation_read`

管理库 `onlyfriends_admin`：

- `admin_user`
- `admin_operation_log`

## 说明

- 所有库表使用 `utf8mb4` / `utf8mb4_unicode_ci`。
- 用户 `email`、`nickname` 与管理员 `username` 等身份字段建有唯一索引。
- `status`、`created_at`、`activity_id`、`user_id`、`team_id`、`conv_id` 等常用查询字段已建索引。
- AI Mock 服务无独立数据库；活动 AI 审核结果写入 `onlyfriends_activity.activity_review_record`。
- 各服务 `resources/db/migration/` 含 Flyway 脚本。通过 `set-local-env.ps1` 或 `start-all.ps1` 启动时默认 `FLYWAY_ENABLED=true`；在 IDE 中直接运行主类且未加载环境变量时，各服务 `application.yml` 默认 `FLYWAY_ENABLED=false`。首次本地开发请先执行 `init-all.sql`。
