-- ============================================================
-- V17：主题选集——策展式组诗，支持按选集连播
-- ============================================================

CREATE TABLE collection (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(50) NOT NULL,
  subtitle VARCHAR(100) DEFAULT '',
  intro TEXT,
  mood VARCHAR(20) DEFAULT '',
  sort INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE collection_poem (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  collection_id BIGINT NOT NULL,
  poem_id BIGINT NOT NULL,
  sort_no INT DEFAULT 0,
  KEY idx_cp_collection (collection_id, sort_no),
  UNIQUE KEY uk_cp (collection_id, poem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO collection (title, subtitle, intro, mood, sort) VALUES
('明月与乡愁','一轮月，照千古离人','从床前明月到千里共婵娟，月亮是中国人共同的信箱。','月夜',1),
('边塞烽烟','大漠孤烟，金戈铁马','出塞、从军、国殇——戍卒的月与将军的血。','家国',2),
('田园与归隐','种豆南山，采菊东篱','把身体交给土地，把心交给自然。','田园',3),
('长恨与深情','此情可待成追忆','从上古誓言到纳兰一叹，深情者的独白。','怀人',4),
('壮志与家国','铁马冰河，丹心汗青','他们把失意与不屈，都写成了山河。','家国',5),
('四季流转','人间寒暑皆成诗','春晓、夏荷、秋思、冬梅，一年的风景都在诗里。','闲适',6);

INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn
FROM collection c
JOIN (
  SELECT 1 cid, 3 poem_id, 1 rn UNION ALL SELECT 1, 13, 2 UNION ALL SELECT 1, 18, 3 UNION ALL
  SELECT 1, 78, 4 UNION ALL SELECT 1, 17, 5 UNION ALL SELECT 1, 96, 6
  UNION ALL SELECT 2, 33, 1 UNION ALL SELECT 2, 35, 2 UNION ALL SELECT 2, 75, 3 UNION ALL
  SELECT 2, 94, 4 UNION ALL SELECT 2, 64, 5
  UNION ALL SELECT 3, 1, 1 UNION ALL SELECT 3, 2, 2 UNION ALL SELECT 3, 12, 3 UNION ALL
  SELECT 3, 30, 4 UNION ALL SELECT 3, 57, 5 UNION ALL SELECT 3, 67, 6 UNION ALL SELECT 3, 80, 7
  UNION ALL SELECT 4, 69, 1 UNION ALL SELECT 4, 70, 2 UNION ALL SELECT 4, 37, 3 UNION ALL
  SELECT 4, 38, 4 UNION ALL SELECT 4, 39, 5 UNION ALL SELECT 4, 45, 6 UNION ALL
  SELECT 4, 47, 7 UNION ALL SELECT 4, 49, 8 UNION ALL SELECT 4, 60, 9 UNION ALL SELECT 4, 102, 10
  UNION ALL SELECT 5, 64, 1 UNION ALL SELECT 5, 11, 2 UNION ALL SELECT 5, 24, 3 UNION ALL
  SELECT 5, 58, 4 UNION ALL SELECT 5, 59, 5 UNION ALL SELECT 5, 110, 6 UNION ALL
  SELECT 5, 111, 7 UNION ALL SELECT 5, 119, 8
  UNION ALL SELECT 6, 29, 1 UNION ALL SELECT 6, 84, 2 UNION ALL SELECT 6, 15, 3 UNION ALL
  SELECT 6, 105, 4 UNION ALL SELECT 6, 112, 5 UNION ALL SELECT 6, 41, 6 UNION ALL SELECT 6, 113, 7
) t ON t.cid = c.id;
