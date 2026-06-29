# Database Initialization

This project keeps one database per backend service in local development:

- `onlyfriends_user`
- `onlyfriends_activity`
- `onlyfriends_social`
- `onlyfriends_im`
- `onlyfriends_admin`

The unified initialization entry is:

```text
sql/init-all.sql
```

Historical module SQL files are still kept under `sql/` for reference and for module-by-module initialization. For a new local environment, prefer `sql/init-all.sql` because it contains all currently implemented tables and baseline data in one ordered script.

## Prerequisites

Start MySQL first:

```powershell
docker compose up -d mysql
```

The default local root password from `docker-compose.yml` is:

```text
onlyfriends_root_password
```

If you set `MYSQL_ROOT_PASSWORD`, use that value instead.

## Run The Unified Script

PowerShell with the Docker MySQL container:

```powershell
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4
```

Local MySQL client:

```powershell
mysql -h 127.0.0.1 -P 3306 -uroot -p --default-character-set=utf8mb4 < .\sql\init-all.sql
```

The script is intended for a fresh local development database. It drops and recreates the current service tables, so do not run it against data you need to keep.

## Initialized Baseline Data

The script inserts:

- Activity templates in `onlyfriends_activity.activity_template`.
- Default admin account in `onlyfriends_admin.admin_user`.

Default admin login:

```text
username: admin
password: Admin123456
```

The database stores only the BCrypt password hash, not the plaintext password.

## Included Tables

User service database `onlyfriends_user`:

- `user`
- `merchant_info`
- `merchant_apply`
- `user_ban_record`

Activity service database `onlyfriends_activity`:

- `activity_template`
- `activity`
- `activity_review_record`
- `activity_registration`
- `activity_waitlist`
- `notification`

Social service database `onlyfriends_social`:

- `user_follow`
- `friend_relation`
- `friend_apply`
- `team`
- `team_member`
- `team_join_apply`
- `team_admin_operation_log`

IM service database `onlyfriends_im`:

- `im_conversation`
- `im_message`
- `im_conversation_read`

Admin service database `onlyfriends_admin`:

- `admin_user`
- `admin_operation_log`

## Notes

- All databases and tables use `utf8mb4` with `utf8mb4_unicode_ci`.
- Unique indexes are configured for account identity fields such as user `email`, user `nickname`, and admin `username`.
- Common query fields such as `status`, `created_at`, `activity_id`, `user_id`, `team_id`, and `conv_id` are indexed.
- The AI mock service does not own a separate database table in the current codebase. Activity AI review results are persisted in `onlyfriends_activity.activity_review_record`.
