-- ============================================================
-- V18：诗人关系图谱——师承 / 挚友 / 交游 / 并称 / 对手 / 文脉
-- ============================================================

CREATE TABLE poet_relation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  poet_id BIGINT NOT NULL,
  related_id BIGINT NOT NULL,
  rel_type VARCHAR(20) NOT NULL,
  note VARCHAR(100) DEFAULT '',
  KEY idx_rel_poet (poet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 仅精选层诗人之间的关系（名字去重定位 id）
INSERT INTO poet_relation (poet_id, related_id, rel_type, note)
SELECT a.id, b.id, t.rel, t.note
FROM (
  SELECT '李白' n1, '孟浩然' n2, '倾慕' rel, '吾爱孟夫子，风流天下闻' note UNION ALL
  SELECT '李白', '杜甫', '挚友', '醉眠秋共被，携手日同行' UNION ALL
  SELECT '李白', '王昌龄', '挚友', '我寄愁心与明月，随风直到夜郎西' UNION ALL
  SELECT '李白', '高适', '交游', '梁宋之游，三人同席' UNION ALL
  SELECT '杜甫', '高适', '交游', '梁宋之游，三人同席' UNION ALL
  SELECT '王昌龄', '高适', '交游', '旗亭画壁，赌诗争胜' UNION ALL
  SELECT '王昌龄', '王之涣', '交游', '旗亭画壁，赌诗争胜' UNION ALL
  SELECT '高适', '王之涣', '交游', '旗亭画壁，赌诗争胜' UNION ALL
  SELECT '王维', '孟浩然', '并称', '王孟，山水田园同调' UNION ALL
  SELECT '陶渊明', '王维', '文脉', '田园一脉，隔代相承' UNION ALL
  SELECT '陶渊明', '苏轼', '文脉', '东坡和陶诗百余首' UNION ALL
  SELECT '白居易', '元稹', '挚友', '元白，诗筒传讯三十年' UNION ALL
  SELECT '白居易', '刘禹锡', '挚友', '刘白，晚岁唱和于洛阳' UNION ALL
  SELECT '刘禹锡', '柳宗元', '挚友', '刘柳，二十三年同贬共勉' UNION ALL
  SELECT '韩愈', '孟郊', '交游', '韩孟联句，奇险相激' UNION ALL
  SELECT '韩愈', '贾岛', '知遇', '推敲之遇，共论诗法' UNION ALL
  SELECT '孟郊', '贾岛', '并称', '郊寒岛瘦' UNION ALL
  SELECT '李商隐', '杜牧', '并称', '小李杜' UNION ALL
  SELECT '晏殊', '欧阳修', '师承', '晏公执卷，欧公继起' UNION ALL
  SELECT '欧阳修', '范仲淹', '同志', '庆历新政，同进同退' UNION ALL
  SELECT '欧阳修', '苏轼', '师承', '老夫当避路，放他出一头地' UNION ALL
  SELECT '欧阳修', '王安石', '知遇', '反复诵常，恐不得见' UNION ALL
  SELECT '苏轼', '秦观', '师承', '苏门四学士之首' UNION ALL
  SELECT '苏轼', '王安石', '对手', '政敌而知己，金陵相会尽释前嫌' UNION ALL
  SELECT '苏轼', '辛弃疾', '文脉', '苏辛，豪放词双峰' UNION ALL
  SELECT '陆游', '辛弃疾', '同志', '南宋恢复双璧，临安曾同朝' UNION ALL
  SELECT '李煜', '纳兰性德', '文脉', '容若词直追后主' UNION ALL
  SELECT '曹操', '曹植', '父子', '建安风骨，三曹之亚' UNION ALL
  SELECT '刘邦', '项羽', '对手', '楚汉相争，一歌一叹' UNION ALL
  SELECT '屈原', '李白', '文脉', '骚人流风，谪仙狂想'
) t
JOIN poet a ON a.name = t.n1 AND a.tier = 1
JOIN poet b ON b.name = t.n2 AND b.tier = 1
WHERE a.id != b.id
  AND NOT EXISTS (SELECT 1 FROM poet_relation r
                  WHERE (r.poet_id = a.id AND r.related_id = b.id)
                     OR (r.poet_id = b.id AND r.related_id = a.id));
