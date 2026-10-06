-- ============================================================
-- V29：关注系统——单向关注（不同于好友的双向关系）
-- ============================================================

CREATE TABLE user_follow (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  follower_id BIGINT NOT NULL COMMENT '关注者',
  following_id BIGINT NOT NULL COMMENT '被关注者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_follow (follower_id, following_id),
  KEY idx_following (following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
