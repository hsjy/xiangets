-- ============================================================
-- V26：补录 V25 漏网的 10 首精选诗（追加到对应选集尾部）
-- ============================================================

INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id,
       (SELECT COALESCE(MAX(sort_no),0) FROM collection_poem x WHERE x.collection_id = c.id) +
       ROW_NUMBER() OVER (PARTITION BY t.cid ORDER BY t.poem_id)
FROM collection c
JOIN (
  SELECT 1 cid, 5 poem_id UNION ALL SELECT 1, 17 UNION ALL SELECT 1, 53 UNION ALL SELECT 1, 83
  UNION ALL SELECT 4 cid, 5 poem_id UNION ALL SELECT 4, 83 UNION ALL SELECT 4, 101
  UNION ALL SELECT 6 cid, 14 poem_id UNION ALL SELECT 6, 16 UNION ALL SELECT 6, 17 UNION ALL
  SELECT 6, 41 UNION ALL SELECT 6, 54
  UNION ALL SELECT 8 cid, 48 poem_id
  UNION ALL SELECT 9 cid, 54 poem_id
) t ON t.cid = c.id
JOIN collection cc ON cc.id = c.id
WHERE NOT EXISTS (SELECT 1 FROM collection_poem x
                  WHERE x.collection_id = c.id AND x.poem_id = t.poem_id)
  AND ((c.title = '明月与乡愁' AND t.cid = 1)
    OR (c.title = '长恨与深情' AND t.cid = 4)
    OR (c.title = '四季流转' AND t.cid = 6)
    OR (c.title = '长亭古道' AND t.cid = 8)
    OR (c.title = '哲思与怀古' AND t.cid = 9));
