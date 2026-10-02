-- ============================================================
-- V7：comment.song_id 允许为空——感悟挂在诗上时无 song_id
-- ============================================================
ALTER TABLE `comment` MODIFY COLUMN `song_id` BIGINT NULL COMMENT '旧曲评论；感悟（poem 评论）为 NULL';
