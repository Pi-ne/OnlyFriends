-- Run against onlyfriends_user when Flyway is disabled.
-- Example: mysql -uroot -p onlyfriends_user < backend/sql/migrate-wechat-login.sql

USE onlyfriends_user;

ALTER TABLE `user`
    MODIFY COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT 'login email, null for WeChat-only users',
    MODIFY COLUMN `password_hash` VARCHAR(255) DEFAULT NULL COMMENT 'BCrypt password hash, null for WeChat-only users',
    ADD COLUMN `wx_openid` VARCHAR(64) DEFAULT NULL COMMENT 'WeChat mini program openid' AFTER `password_hash`,
    ADD COLUMN `wx_unionid` VARCHAR(64) DEFAULT NULL COMMENT 'WeChat unionid' AFTER `wx_openid`,
    ADD UNIQUE KEY `uk_user_wx_openid` (`wx_openid`);
