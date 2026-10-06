-- ============================================================
-- V22：感悟互动——回复（一级）与点赞
-- ============================================================

ALTER TABLE comment
  ADD COLUMN parent_id BIGINT NULL COMMENT '父感悟；回复指向根感悟' AFTER user_id,
  ADD KEY idx_comment_parent (parent_id);

CREATE TABLE comment_like (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  comment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_comment_like (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
