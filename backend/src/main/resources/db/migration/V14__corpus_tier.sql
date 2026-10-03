-- ============================================================
-- V14：内容分层——poem/poet 增加 tier（1 精选 / 2 语料）
-- 精选层带注释/译文/赏析，语料层为批量导入的诗文全文
-- ============================================================

ALTER TABLE poem ADD COLUMN tier TINYINT NOT NULL DEFAULT 1 AFTER famous_line;
ALTER TABLE poet ADD COLUMN tier TINYINT NOT NULL DEFAULT 1 AFTER story_title;
CREATE INDEX idx_poem_tier ON poem (tier, id);
CREATE INDEX idx_poet_tier ON poet (tier, id);
