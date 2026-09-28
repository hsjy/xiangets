-- ============================================================
-- 悦听 · 在线音乐播放网站 —— MySQL 建表脚本
-- 对应技术选型中的核心表：user / singer / song / playlist / playlist_song
-- 另含 comment、user_favorite、song_like 三个辅助表
-- ============================================================

CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码（SHA-256 + 盐）',
  `name`        VARCHAR(50)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(500)          DEFAULT NULL COMMENT '头像地址',
  `email`       VARCHAR(100)         DEFAULT NULL COMMENT '邮箱',
  `phone`       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
  `sex`         TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
  `birth`       VARCHAR(20)           DEFAULT NULL COMMENT '生日',
  `intro`       VARCHAR(255)          DEFAULT NULL COMMENT '个性签名',
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色：user/admin',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `singer` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '歌手ID',
  `name`        VARCHAR(50)  NOT NULL COMMENT '歌手名',
  `gender`      TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
  `avatar`      VARCHAR(500)          DEFAULT NULL COMMENT '歌手头像',
  `style`       VARCHAR(50)           DEFAULT NULL COMMENT '主要风格',
  `location`    VARCHAR(50)           DEFAULT NULL COMMENT '地区',
  `birth`       VARCHAR(20)           DEFAULT NULL COMMENT '出生日期',
  `intro`       VARCHAR(500)          DEFAULT NULL COMMENT '简介',
  `song_count`  INT          NOT NULL DEFAULT 0 COMMENT '歌曲数（冗余统计）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_singer_name` (`name`),
  KEY `idx_singer_style` (`style`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌手表';

CREATE TABLE IF NOT EXISTS `song` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '歌曲ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '歌曲名',
  `singer_id`   BIGINT       NOT NULL COMMENT '所属歌手ID',
  `cover`       VARCHAR(500)          DEFAULT NULL COMMENT '封面图',
  `url`         VARCHAR(500) NOT NULL COMMENT '音频地址',
  `duration`    DECIMAL(10,1) NOT NULL DEFAULT 0 COMMENT '时长（秒，保留一位小数）',
  `style`       VARCHAR(50)           DEFAULT NULL COMMENT '曲风',
  `play_count`  BIGINT       NOT NULL DEFAULT 0 COMMENT '播放量',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_song_singer` (`singer_id`),
  KEY `idx_song_style` (`style`),
  KEY `idx_song_plays` (`play_count` DESC),
  CONSTRAINT `fk_song_singer` FOREIGN KEY (`singer_id`) REFERENCES `singer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲表';

CREATE TABLE IF NOT EXISTS `playlist` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '歌单ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '歌单名',
  `intro`       VARCHAR(500)          DEFAULT NULL COMMENT '歌单描述',
  `cover`       VARCHAR(500)          DEFAULT NULL COMMENT '歌单封面',
  `style`       VARCHAR(50)           DEFAULT NULL COMMENT '歌单分类：流行/摇滚/民谣…',
  `creator_id`  BIGINT                DEFAULT NULL COMMENT '创建者ID，NULL 表示官方歌单',
  `is_public`   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否公开（public 是 MySQL 保留字，故用 is_public）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_playlist_creator` (`creator_id`),
  KEY `idx_playlist_style` (`style`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌单表';

-- 歌单-歌曲多对多关联表，sort_no 记录用户自定义播放顺序
CREATE TABLE IF NOT EXISTS `playlist_song` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `playlist_id` BIGINT   NOT NULL COMMENT '歌单ID',
  `song_id`     BIGINT   NOT NULL COMMENT '歌曲ID',
  `sort_no`     INT      NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_playlist_song` (`playlist_id`, `song_id`),
  KEY `idx_ps_song` (`song_id`),
  CONSTRAINT `fk_ps_playlist` FOREIGN KEY (`playlist_id`) REFERENCES `playlist` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ps_song` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌单歌曲关联表';

CREATE TABLE IF NOT EXISTS `comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `song_id`     BIGINT       NOT NULL COMMENT '歌曲ID',
  `user_id`     BIGINT       NOT NULL COMMENT '评论者ID',
  `content`     VARCHAR(500) NOT NULL COMMENT '评论内容',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_song` (`song_id`),
  CONSTRAINT `fk_comment_song` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲评论表';

-- 收藏的歌单（用户 ↔ 歌单 多对多）
CREATE TABLE IF NOT EXISTS `user_favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `playlist_id` BIGINT   NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav` (`user_id`, `playlist_id`),
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fav_playlist` FOREIGN KEY (`playlist_id`) REFERENCES `playlist` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏歌单表';

-- 「我喜欢的音乐」红心
CREATE TABLE IF NOT EXISTS `song_like` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `song_id`     BIGINT   NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like` (`user_id`, `song_id`),
  CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_like_song` FOREIGN KEY (`song_id`) REFERENCES `song` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户喜欢歌曲表';
