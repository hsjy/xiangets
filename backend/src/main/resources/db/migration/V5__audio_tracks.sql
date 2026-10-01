-- ============================================================
-- V5：音频挂载——朗诵者/配乐者实体 + 诗之音轨（朗诵/纯音乐）
-- 音频文件由 tools/build_recite.py 与 tools/synth_bgm.py 生成，
-- 落盘 backend/audio/，经 /api/audio/** 静态服务访问
-- ============================================================

INSERT INTO singer (name, gender, avatar, style, location, intro, song_count) VALUES
  ('云溪（男声·朗诵）',1,NULL,'朗诵','数字乐府','项目合成的男声朗诵者，音色温润，宜诵山水怀人之作。',22),
  ('晓晓（女声·朗诵）',2,NULL,'朗诵','数字乐府','项目合成的女声朗诵者，清丽婉转，宜诵婉约词章。',2),
  ('松涛（合成·古筝）',0,NULL,'古筝','数字乐府','Karplus-Strong 物理建模合成的五声音阶古筝，为诗铺底的纯音乐。',6);

INSERT IGNORE INTO song (name, singer_id, poem_id, track_kind, url, duration, style, play_count) VALUES
  ('《饮酒·其五》配乐朗诵', 1, 1, 'recitation', '/api/audio/recite/poem-1.mp3', 23.9, '田园', 0),
  ('《归园田居·其三》配乐朗诵', 1, 2, 'recitation', '/api/audio/recite/poem-2.mp3', 19.2, '田园', 0),
  ('《静夜思》配乐朗诵', 1, 3, 'recitation', '/api/audio/recite/poem-3.mp3', 10.8, '思乡', 0),
  ('《望庐山瀑布》配乐朗诵', 1, 4, 'recitation', '/api/audio/recite/poem-4.mp3', 12.7, '山水', 0),
  ('《月下独酌·其一》配乐朗诵', 1, 5, 'recitation', '/api/audio/recite/poem-5.mp3', 29.0, '月夜', 0),
  ('《早发白帝城》配乐朗诵', 1, 6, 'recitation', '/api/audio/recite/poem-6.mp3', 12.7, '豪放', 0),
  ('《山居秋暝》配乐朗诵', 1, 7, 'recitation', '/api/audio/recite/poem-7.mp3', 18.2, '山水', 0),
  ('《鹿柴》配乐朗诵', 1, 8, 'recitation', '/api/audio/recite/poem-8.mp3', 10.7, '山水', 0),
  ('《送元二使安西》配乐朗诵', 1, 9, 'recitation', '/api/audio/recite/poem-9.mp3', 13.0, '送别', 0),
  ('《九月九日忆山东兄弟》配乐朗诵', 1, 10, 'recitation', '/api/audio/recite/poem-10.mp3', 13.9, '思乡', 0),
  ('《春望》配乐朗诵', 1, 11, 'recitation', '/api/audio/recite/poem-11.mp3', 17.9, '家国', 0),
  ('《春夜喜雨》配乐朗诵', 1, 12, 'recitation', '/api/audio/recite/poem-12.mp3', 18.4, '田园', 0),
  ('《月夜忆舍弟》配乐朗诵', 1, 13, 'recitation', '/api/audio/recite/poem-13.mp3', 18.0, '月夜', 0),
  ('《登高》配乐朗诵', 1, 14, 'recitation', '/api/audio/recite/poem-14.mp3', 21.2, '悲秋', 0),
  ('《赋得古原草送别》配乐朗诵', 1, 15, 'recitation', '/api/audio/recite/poem-15.mp3', 19.1, '送别', 0),
  ('《钱塘湖春行》配乐朗诵', 1, 16, 'recitation', '/api/audio/recite/poem-16.mp3', 21.6, '山水', 0),
  ('《暮江吟》配乐朗诵', 1, 17, 'recitation', '/api/audio/recite/poem-17.mp3', 12.2, '月夜', 0),
  ('《水调歌头·明月几时有》配乐朗诵', 1, 18, 'recitation', '/api/audio/recite/poem-18.mp3', 37.4, '月夜', 0),
  ('《饮湖上初晴后雨·其二》配乐朗诵', 1, 19, 'recitation', '/api/audio/recite/poem-19.mp3', 14.3, '山水', 0),
  ('《题西林壁》配乐朗诵', 1, 20, 'recitation', '/api/audio/recite/poem-20.mp3', 12.3, '哲理', 0),
  ('《如梦令·常记溪亭日暮》配乐朗诵', 2, 21, 'recitation', '/api/audio/recite/poem-21.mp3', 16.0, '闲适', 0),
  ('《声声慢·寻寻觅觅》配乐朗诵', 2, 22, 'recitation', '/api/audio/recite/poem-22.mp3', 36.6, '怀人', 0),
  ('《西江月·夜行黄沙道中》配乐朗诵', 1, 23, 'recitation', '/api/audio/recite/poem-23.mp3', 21.0, '田园', 0),
  ('《破阵子·为陈同甫赋壮词以寄之》配乐朗诵', 1, 24, 'recitation', '/api/audio/recite/poem-24.mp3', 26.8, '壮志', 0),
  ('《月夜》古筝铺底', 3, NULL, 'bgm', '/api/audio/bgm/bgm-yueye.mp3', 92.0, '月夜', 0),
  ('《田园》古筝铺底', 3, NULL, 'bgm', '/api/audio/bgm/bgm-tianyuan.mp3', 90.0, '田园', 0),
  ('《山水》古筝铺底', 3, NULL, 'bgm', '/api/audio/bgm/bgm-shanshui.mp3', 91.0, '山水', 0),
  ('《送别》古筝铺底', 3, NULL, 'bgm', '/api/audio/bgm/bgm-songbie.mp3', 93.0, '送别', 0),
  ('《壮志》古筝铺底', 3, NULL, 'bgm', '/api/audio/bgm/bgm-zhuangzhi.mp3', 90.0, '壮志', 0),
  ('《怀人》古筝铺底', 3, NULL, 'bgm', '/api/audio/bgm/bgm-huairen.mp3', 92.0, '怀人', 0);