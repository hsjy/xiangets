-- ============================================================
-- V21：登录机制强化
--   密保问题（自助找回密码）+ 登录失败锁定（防爆破）+ 记住我
-- 老用户密保为空：找回密码时会引导重新设置（demo/admin 亦然）
-- ============================================================

ALTER TABLE user
  ADD COLUMN pwd_question VARCHAR(100) NULL COMMENT '密保问题',
  ADD COLUMN pwd_answer   VARCHAR(128) NULL COMMENT '密保答案（散列）',
  ADD COLUMN failed_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  ADD COLUMN locked_until DATETIME NULL COMMENT '锁定截止时间';
