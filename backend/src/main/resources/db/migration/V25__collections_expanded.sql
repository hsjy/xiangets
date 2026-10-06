-- ============================================================
-- V25：选集扩充——9 大主题，121 首精选诗全部纳入（部分诗跨集互见）
-- ============================================================

DELETE FROM collection_poem;

-- 新增三个主题
INSERT INTO collection (title, subtitle, intro, mood, sort) VALUES
('山水行旅','烟霞入眼皆成章','从庐山飞瀑到鹳雀高楼，行踪所至，山水入诗。','山水',7),
('长亭古道','劝君更尽一杯酒','离别是中国诗最古老的母题——古道、残笛与远行的帆。','送别',8),
('哲思与怀古','读史早知今日事','咏史以鉴今，观物以明理——诗是最好的哲学。','哲理',9);

-- 1 明月与乡愁
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 1 cid, 3 poem_id, 1 rn UNION ALL SELECT 1, 10, 2 UNION ALL SELECT 1, 13, 3 UNION ALL
  SELECT 1, 18, 4 UNION ALL SELECT 1, 31, 5 UNION ALL SELECT 1, 77, 6 UNION ALL
  SELECT 1, 78, 7 UNION ALL SELECT 1, 79, 8 UNION ALL SELECT 1, 96, 9 UNION ALL
  SELECT 1, 99, 10 UNION ALL SELECT 1, 117, 11 UNION ALL SELECT 1, 95, 12
) t ON t.cid = c.id WHERE c.title = '明月与乡愁';

-- 2 边塞烽烟
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 2 cid, 33 poem_id, 1 rn UNION ALL SELECT 2, 35, 2 UNION ALL SELECT 2, 63, 3 UNION ALL
  SELECT 2, 73, 4 UNION ALL SELECT 2, 75, 5 UNION ALL SELECT 2, 82, 6 UNION ALL
  SELECT 2, 94, 7 UNION ALL SELECT 2, 64, 8 UNION ALL SELECT 2, 98, 9
) t ON t.cid = c.id WHERE c.title = '边塞烽烟';

-- 3 田园与归隐
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 3 cid, 1 poem_id, 1 rn UNION ALL SELECT 3, 2, 2 UNION ALL SELECT 3, 7, 3 UNION ALL
  SELECT 3, 8, 4 UNION ALL SELECT 3, 12, 5 UNION ALL SELECT 3, 23, 6 UNION ALL
  SELECT 3, 30, 7 UNION ALL SELECT 3, 57, 8 UNION ALL SELECT 3, 67, 9 UNION ALL
  SELECT 3, 80, 10 UNION ALL SELECT 3, 85, 11 UNION ALL SELECT 3, 86, 12 UNION ALL
  SELECT 3, 115, 13
) t ON t.cid = c.id WHERE c.title = '田园与归隐';

-- 4 长恨与深情
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 4 cid, 37 poem_id, 1 rn UNION ALL SELECT 4, 38, 2 UNION ALL SELECT 4, 39, 3 UNION ALL
  SELECT 4, 47, 4 UNION ALL SELECT 4, 49, 5 UNION ALL SELECT 4, 51, 6 UNION ALL
  SELECT 4, 60, 7 UNION ALL SELECT 4, 61, 8 UNION ALL SELECT 4, 62, 9 UNION ALL
  SELECT 4, 69, 10 UNION ALL SELECT 4, 70, 11 UNION ALL SELECT 4, 71, 12 UNION ALL
  SELECT 4, 91, 13 UNION ALL SELECT 4, 92, 14 UNION ALL SELECT 4, 102, 15 UNION ALL
  SELECT 4, 103, 16 UNION ALL SELECT 4, 116, 17 UNION ALL SELECT 4, 22, 18
) t ON t.cid = c.id WHERE c.title = '长恨与深情';

-- 5 壮志与家国
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 5 cid, 11 poem_id, 1 rn UNION ALL SELECT 5, 24, 2 UNION ALL SELECT 5, 26, 3 UNION ALL
  SELECT 5, 27, 4 UNION ALL SELECT 5, 28, 5 UNION ALL SELECT 5, 43, 6 UNION ALL
  SELECT 5, 58, 7 UNION ALL SELECT 5, 59, 8 UNION ALL SELECT 5, 64, 9 UNION ALL
  SELECT 5, 65, 10 UNION ALL SELECT 5, 66, 11 UNION ALL SELECT 5, 93, 12 UNION ALL
  SELECT 5, 95, 13 UNION ALL SELECT 5, 97, 14 UNION ALL SELECT 5, 109, 15 UNION ALL
  SELECT 5, 110, 16 UNION ALL SELECT 5, 111, 17 UNION ALL SELECT 5, 114, 18 UNION ALL
  SELECT 5, 118, 19 UNION ALL SELECT 5, 119, 20
) t ON t.cid = c.id WHERE c.title = '壮志与家国';

-- 6 四季流转
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 6 cid, 21 poem_id, 1 rn UNION ALL SELECT 6, 29, 2 UNION ALL SELECT 6, 42, 3 UNION ALL
  SELECT 6, 56, 4 UNION ALL SELECT 6, 76, 5 UNION ALL SELECT 6, 84, 6 UNION ALL
  SELECT 6, 87, 7 UNION ALL SELECT 6, 105, 8 UNION ALL SELECT 6, 106, 9 UNION ALL
  SELECT 6, 107, 10 UNION ALL SELECT 6, 108, 11 UNION ALL SELECT 6, 112, 12 UNION ALL
  SELECT 6, 113, 13 UNION ALL SELECT 6, 36, 14
) t ON t.cid = c.id WHERE c.title = '四季流转';

-- 7 山水行旅（新）
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 7 cid, 4 poem_id, 1 rn UNION ALL SELECT 7, 6, 2 UNION ALL SELECT 7, 19, 3 UNION ALL
  SELECT 7, 20, 4 UNION ALL SELECT 7, 25, 5 UNION ALL SELECT 7, 32, 6 UNION ALL
  SELECT 7, 46, 7 UNION ALL SELECT 7, 50, 8 UNION ALL SELECT 7, 74, 9 UNION ALL
  SELECT 7, 88, 10
) t ON t.cid = c.id WHERE c.title = '山水行旅';

-- 8 长亭古道（新）
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 8 cid, 9 poem_id, 1 rn UNION ALL SELECT 8, 15, 2 UNION ALL SELECT 8, 34, 3 UNION ALL
  SELECT 8, 45, 4 UNION ALL SELECT 8, 81, 5
) t ON t.cid = c.id WHERE c.title = '长亭古道';

-- 9 哲思与怀古（新）
INSERT INTO collection_poem (collection_id, poem_id, sort_no)
SELECT c.id, t.poem_id, t.rn FROM collection c JOIN (
  SELECT 9 cid, 40 poem_id, 1 rn UNION ALL SELECT 9, 44, 2 UNION ALL SELECT 9, 52, 3 UNION ALL
  SELECT 9, 55, 4 UNION ALL SELECT 9, 68, 5 UNION ALL SELECT 9, 72, 6 UNION ALL
  SELECT 9, 89, 7 UNION ALL SELECT 9, 90, 8 UNION ALL SELECT 9, 100, 9 UNION ALL
  SELECT 9, 104, 10 UNION ALL SELECT 9, 120, 11 UNION ALL SELECT 9, 121, 12
) t ON t.cid = c.id WHERE c.title = '哲思与怀古';
