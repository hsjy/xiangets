-- ============================================================
-- V28：好友系统——加友请求、互相关注、好友间私信
-- ============================================================

CREATE TABLE user_friend (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '发起方',
  friend_id BIGINT NOT NULL COMMENT '被邀请方',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/accepted',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_friend_pair (user_id, friend_id),
  KEY idx_friend_user (user_id, status),
  KEY idx_friend_target (friend_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE friend_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_fm_pair (sender_id, receiver_id),
  KEY idx_fm_receiver (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
