-- ============================================================
-- V3：以诗为主体——诗人表、诗表；歌曲挂载到诗（音乐辅助化）
-- ============================================================

-- 诗人（含生平与诗人故事，故事是产品的一级内容）
CREATE TABLE IF NOT EXISTS `poet` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '诗人ID',
  `name`        VARCHAR(50)  NOT NULL COMMENT '姓名',
  `zi`          VARCHAR(50)           DEFAULT NULL COMMENT '字',
  `hao`         VARCHAR(50)           DEFAULT NULL COMMENT '号',
  `dynasty`     VARCHAR(20)  NOT NULL COMMENT '朝代：魏晋/唐/宋…',
  `years`       VARCHAR(30)           DEFAULT NULL COMMENT '生卒年，如 701-762',
  `title`       VARCHAR(50)           DEFAULT NULL COMMENT '称号，如 诗仙/词宗',
  `intro`       VARCHAR(500)          DEFAULT NULL COMMENT '生平概述',
  `story`       TEXT                  DEFAULT NULL COMMENT '诗人故事（叙事长文）',
  `story_title` VARCHAR(100)          DEFAULT NULL COMMENT '故事标题',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_poet_name_dynasty` (`name`, `dynasty`),
  KEY `idx_poet_dynasty` (`dynasty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诗人表';

-- 诗（欣赏主体：诗行/注/译/赏/情绪/体裁）
CREATE TABLE IF NOT EXISTS `poem` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '诗ID',
  `title`        VARCHAR(100) NOT NULL COMMENT '诗题',
  `subtitle`     VARCHAR(100)          DEFAULT NULL COMMENT '副题，如 其三 / ·明月几时有',
  `poet_id`      BIGINT       NOT NULL COMMENT '诗人ID',
  `dynasty`      VARCHAR(20)  NOT NULL COMMENT '朝代（冗余自诗人，便于筛选）',
  `genre`        VARCHAR(20)           DEFAULT NULL COMMENT '体裁：五言绝句/七言律诗/词…',
  `paragraphs`   TEXT         NOT NULL COMMENT '诗行，换行分隔',
  `annotation`   TEXT                  DEFAULT NULL COMMENT '注释（难字词）',
  `translation`  TEXT                  DEFAULT NULL COMMENT '白话译文',
  `appreciation` TEXT                  DEFAULT NULL COMMENT '赏析',
  `mood`         VARCHAR(20)           DEFAULT NULL COMMENT '情绪意象：月夜/田园/送别/边塞…',
  `famous_line`  VARCHAR(100)          DEFAULT NULL COMMENT '名句（今日一诗签用）',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_poem_poet` (`poet_id`),
  KEY `idx_poem_dynasty` (`dynasty`),
  KEY `idx_poem_mood` (`mood`),
  CONSTRAINT `fk_poem_poet` FOREIGN KEY (`poet_id`) REFERENCES `poet` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诗表';

-- 音乐辅助化：歌曲可挂载到诗（朗诵配乐/纯音乐/AI 谱曲都服务于某首诗）
ALTER TABLE `song`
  ADD COLUMN `poem_id` BIGINT NULL COMMENT '关联的诗ID；NULL=纯氛围曲目' AFTER `id`,
  ADD COLUMN `track_kind` VARCHAR(20) NULL COMMENT 'recitation朗诵配乐/bgm纯音乐/ai生成' AFTER `poem_id`,
  ADD KEY `idx_song_poem` (`poem_id`),
  ADD CONSTRAINT `fk_song_poem` FOREIGN KEY (`poem_id`) REFERENCES `poem` (`id`) ON DELETE SET NULL;
