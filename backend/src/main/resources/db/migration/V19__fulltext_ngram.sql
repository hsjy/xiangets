-- ============================================================
-- V19：中文搜索升级——ngram 全文索引（默认 bigram 分词）
-- 搜索单字时服务端回退 LIKE
-- ============================================================

CREATE FULLTEXT INDEX ft_poem ON poem (title, subtitle, paragraphs) WITH PARSER ngram;
