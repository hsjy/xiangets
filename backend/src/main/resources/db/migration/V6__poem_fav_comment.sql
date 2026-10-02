-- ============================================================
-- V6：诗的收藏与感悟——欣赏行为的落点
-- ============================================================

-- 感悟（评论）可挂在诗上；song_id 与 poem_id 至少其一
ALTER TABLE `comment`
  ADD COLUMN `poem_id` BIGINT NULL COMMENT '关联的诗；NULL=旧曲评论' AFTER `song_id`,
  ADD KEY `idx_comment_poem` (`poem_id`),
  ADD CONSTRAINT `fk_comment_poem` FOREIGN KEY (`poem_id`) REFERENCES `poem` (`id`) ON DELETE CASCADE;

-- 收藏的诗（书斋）
CREATE TABLE IF NOT EXISTS `poem_favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `poem_id`     BIGINT   NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_poem_fav` (`user_id`, `poem_id`),
  CONSTRAINT `fk_pfav_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pfav_poem` FOREIGN KEY (`poem_id`) REFERENCES `poem` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏的诗';
