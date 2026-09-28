-- ============================================================
-- 悦听 · 初始化数据（与前端 mock 层数据保持一致）
-- 演示账号：demo / 123456 ，admin / admin123
-- 密码为明文，仅用于本地初始化；后端登录时统一做 SHA-256 加盐比对
-- ============================================================

INSERT INTO `user` (`id`,`username`,`password`,`name`,`avatar`,`email`,`phone`,`sex`,`birth`,`intro`,`role`,`create_time`) VALUES
  (1,'demo','123456','演示用户','https://api.dicebear.com/7.x/adventurer/svg?seed=demo&backgroundColor=b6e3f4,c0aede,d1d4f9','demo@yuetin.example','13800000000',0,'2000-01-01','这个人很懒，但很爱听歌。','user','2024-01-10 02:00:00'),
  (2,'admin','admin123','管理员','https://api.dicebear.com/7.x/adventurer/svg?seed=admin&backgroundColor=b6e3f4,c0aede,d1d4f9','admin@yuetin.example','',1,'','站点管理员。','admin','2024-01-10 02:00:00');

INSERT INTO `singer` (`id`,`name`,`gender`,`avatar`,`style`,`location`,`birth`,`intro`,`song_count`,`create_time`) VALUES
  (1,'林晚晴',1,NULL,'流行','浙江 杭州','1996-04-18','声音干净温暖的创作型歌手，擅长把日常心事写成旋律。代表作《晚风信》《时光邮局》。',2,'2024-01-01 00:00:00'),
  (2,'星尘乐团',2,NULL,'摇滚','北京','2015-09-01','五人制独立摇滚乐团，以恢弘编曲与少年感歌词著称，现场感染力极强。',2,'2024-01-01 00:00:00'),
  (3,'陈叙白',0,NULL,'民谣','四川 成都','1990-11-06','行走过的城市都写进了歌里。一把木吉他，唱尽小镇与远方。',2,'2024-01-01 00:00:00'),
  (4,'苏酥',1,NULL,'流行','上海','1999-07-22','甜而不腻的嗓音自带气泡感，是夏日限定的快乐源泉。',2,'2024-01-01 00:00:00'),
  (5,'慕辰',0,NULL,'电子','广东 深圳','1993-02-14','电子音乐制作人，专注合成器美学，在霓虹与深海之间构建声场。',2,'2024-01-01 00:00:00'),
  (6,'顾云深',0,NULL,'古风','陕西 西安','1988-12-03','以山水入词、以丝竹入曲，用现代编曲复活古典意境。',2,'2024-01-01 00:00:00'),
  (7,'夏未至',1,NULL,'R&B','湖南 长沙','1997-05-30','律动至上的 R&B 新声代，唱腔慵懒，句句都是深夜心事。',2,'2024-01-01 00:00:00'),
  (8,'岚烟',1,NULL,'古风','江苏 苏州','1995-08-09','江南女子，歌声如画。一笔一墨皆是山河岁月。',2,'2024-01-01 00:00:00');

INSERT INTO `song` (`id`,`name`,`singer_id`,`cover`,`url`,`duration`,`style`,`play_count`,`create_time`) VALUES
  (1,'晚风信',1,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',372.7,'流行',284567,'2024-01-13 02:00:00'),
  (2,'时光邮局',1,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',425.9,'流行',156234,'2024-01-16 02:00:00'),
  (3,'银河漫游',2,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',344,'摇滚',512081,'2024-01-19 02:00:00'),
  (4,'逆光飞行',2,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',302.5,'摇滚',98765,'2024-01-22 02:00:00'),
  (5,'南方小镇',3,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',353.9,'民谣',342190,'2024-01-25 02:00:00'),
  (6,'旧巷',3,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',279.6,'民谣',77432,'2024-01-28 02:00:00'),
  (7,'甜品店',4,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',420.6,'流行',423675,'2024-01-31 02:00:00'),
  (8,'夏日气泡',4,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',325.3,'流行',654303,'2024-02-03 02:00:00'),
  (9,'霓虹雨',5,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',389.3,'电子',231456,'2024-02-06 02:00:00'),
  (10,'深空电台',5,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3',527.3,'电子',188932,'2024-02-09 02:00:00'),
  (11,'墨色山水',6,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3',551,'古风',76543,'2024-02-12 02:00:00'),
  (12,'长安引',6,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3',513.6,'古风',334567,'2024-02-15 02:00:00'),
  (13,'深夜便利店',7,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3',470,'R&B',456789,'2024-02-18 02:00:00'),
  (14,'心动信号',7,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3',516.2,'R&B',567891,'2024-02-21 02:00:00'),
  (15,'山河图',8,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3',438,'古风',123456,'2024-02-24 02:00:00'),
  (16,'画中人',8,NULL,'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3',483.4,'古风',210987,'2024-02-27 02:00:00');

INSERT INTO `playlist` (`id`,`name`,`intro`,`cover`,`style`,`creator_id`,`is_public`,`create_time`) VALUES
  (1,'深夜学习时段','适合专注的安静旋律，让思绪随音乐沉淀。','https://picsum.photos/seed/yuetin-playlist-1/400','学习',NULL,1,'2024-01-10 02:00:00'),
  (2,'通勤路上的轻快旋律','早高峰也要有好心情，节奏刚刚好的歌单。','https://picsum.photos/seed/yuetin-playlist-2/400','通勤',NULL,1,'2024-01-20 02:00:00'),
  (3,'雨夜古风精选','窗外落雨，室内茶香，一曲古风入梦来。','https://picsum.photos/seed/yuetin-playlist-3/400','古风',NULL,1,'2024-02-04 02:00:00'),
  (4,'电子音浪派对','合成器浪潮来袭，重低音爱好者请进。','https://picsum.photos/seed/yuetin-playlist-4/400','电子',NULL,1,'2024-02-19 02:00:00'),
  (5,'华语流行新歌潮','近期最值得一听的华语流行作品合集。','https://picsum.photos/seed/yuetin-playlist-5/400','流行',NULL,1,'2024-03-05 02:00:00'),
  (6,'睡前轻音','温柔收官这一天，愿你好梦。','https://picsum.photos/seed/yuetin-playlist-6/400','助眠',NULL,1,'2024-03-20 02:00:00'),
  (7,'夜跑节奏站','','https://picsum.photos/seed/yuetin-upl-7-1/400','我的收藏',1,1,'2026-09-27 03:09:07');

INSERT INTO `playlist_song` (`playlist_id`,`song_id`,`sort_no`) VALUES
  (1,1,5),
  (1,4,5),
  (1,6,5),
  (1,8,5),
  (1,12,5),
  (2,2,5),
  (2,3,5),
  (2,7,5),
  (2,8,5),
  (2,14,5),
  (3,6,5),
  (3,11,5),
  (3,12,5),
  (3,15,5),
  (3,16,5),
  (4,3,4),
  (4,9,4),
  (4,10,4),
  (4,13,4),
  (5,1,6),
  (5,2,6),
  (5,7,6),
  (5,8,6),
  (5,13,6),
  (5,14,6),
  (6,5,4),
  (6,11,4),
  (6,12,4),
  (6,16,4),
  (7,8,1);

INSERT INTO `comment` (`song_id`,`user_id`,`content`,`create_time`) VALUES
  (1,1,'前奏一响就像收到了一封夏天的信！','2024-01-22 02:00:00'),
  (3,2,'副歌的吉他太顶了，单曲循环中。','2024-01-30 02:00:00');

INSERT INTO `user_favorite` (`user_id`,`playlist_id`) VALUES
  (1,1),
  (1,3);
