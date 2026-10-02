-- ============================================================
-- V8：AI 谱曲任务表——「古诗转音乐」的任务与缓存
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_music` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `poem_id`     BIGINT       NOT NULL COMMENT '目标诗',
  `provider`    VARCHAR(30)  NOT NULL DEFAULT 'preset' COMMENT 'preset预生成/acestep真实生成',
  `prompt`      VARCHAR(500)          DEFAULT NULL COMMENT '风格提示词（意象映射）',
  `lyrics`      TEXT                  DEFAULT NULL COMMENT '歌词（诗行原文）',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/running/done/failed',
  `error`       VARCHAR(500)          DEFAULT NULL,
  `track_id`    BIGINT                DEFAULT NULL COMMENT '生成完成后的音轨ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finish_time` DATETIME              DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_poem` (`poem_id`),
  CONSTRAINT `fk_ai_poem` FOREIGN KEY (`poem_id`) REFERENCES `poem` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 谱曲任务';
