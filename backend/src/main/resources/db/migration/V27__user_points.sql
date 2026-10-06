-- ============================================================
-- V27：用户积分——飞花令对句得分，管理后台可查看与调整
-- ============================================================

ALTER TABLE user ADD COLUMN points INT NOT NULL DEFAULT 0 COMMENT '积分' AFTER locked_until;
