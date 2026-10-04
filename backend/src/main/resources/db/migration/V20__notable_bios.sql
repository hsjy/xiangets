-- ============================================================
-- V20：名家小传补遗——元曲四大家与花间词人群（语料自带小传覆盖不到）
-- ============================================================

UPDATE poet SET intro='元杂剧奠基人，元曲四大家之首。大都人，号己斋叟，一生不仕，长期混迹勾栏瓦舍，为伶人写戏。今知所作杂剧六十余种，存《窦娥冤》《救风尘》《单刀会》等十八种。以「蒸不烂、煮不熟、捶不扁、炒不爆、响珰珰一粒铜豌豆」自况，笔锋直指吏治黑暗。' WHERE name='关汉卿' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元曲四大家之一，字太素，号兰谷，隩州（今山西河曲）人。金亡后不仕，放情山水诗酒。杂剧《梧桐雨》写唐明皇与杨贵妃事，凄婉千古；《墙头马上》是元杂剧爱情戏名篇。散曲清丽淡雅，后人有「曲中李杜」之目。' WHERE name='白朴' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元曲四大家之一，字德辉，平阳襄陵（今山西临汾）人。以儒补杭州路吏，为人方直，不与人交。杂剧《倩女离魂》以「魂随人去」写至情，是元代爱情剧杰作。' WHERE name='郑光祖' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元代杂剧大家，《西厢记》作者。大都人，一说金末生人。《西厢记》以「愿普天下有情的都成了眷属」为主旨，语言华美，人物生动，被誉「天下夺魁」，是元杂剧的压卷之作。' WHERE name='王实甫' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元散曲后期大家，字梦符，太原人，号惺惺道人，流寓杭州。散曲清丽新奇，与张可久并称「张乔」，有「曲中李商隐」之称。' WHERE name='乔吉' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元散曲清丽派领袖，字小山，庆元（今浙江宁波）人。一生不得志，浪迹江湖。存散曲八百余首，为元人之冠，清丽典雅，后人辑为《小山乐府》。' WHERE name='张可久' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元散曲家，字希孟，号云庄，济南人。官至礼部尚书、参议中书省事，后弃官归隐。天历二年关中大旱，复出赈灾，积劳而卒。一曲《山坡羊·潼关怀古》，「兴，百姓苦；亡，百姓苦」为千古绝唱。' WHERE name='张养浩' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元散曲家，字景贤，扬州人。《般涉调·哨遍·高祖还乡》以乡民视角解构帝王仪仗，尖新泼辣，是元散曲讽刺名篇。' WHERE name='睢景臣' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='元散曲家，字德可，嘉兴人，号甜斋。散曲清丽雅致，以《折桂令·春情》「平生不会相思」著名。' WHERE name='徐再思' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='唐代诗人、词人，字飞卿，太原人。八叉手而成八韵，时号「温八叉」。才思艳丽，为「花间词派」鼻祖，词收《花间集》首位，与韦庄并称「温韦」，词风「香而软」。' WHERE name='温庭筠' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='唐末五代诗人、词人，字端己，京兆杜陵人。长篇叙事诗《秦妇吟》为唐代最长诗，与《长恨歌》并称。晚年入蜀为王建宰相，词清丽疏朗，与温庭筠并称「温韦」。' WHERE name='韦庄' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='五代词人，字成绩，郓州须昌人。历仕后梁、后唐、后晋、后汉、后周五朝，官至宰相。词多艳情小令，收《花间集》。' WHERE name='和凝' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='五代词人，字孟文，陵州贵平人。历仕南平三世，官至御史中丞。词风清丽疏淡，题材较广，《花间集》《尊前集》均收其作。' WHERE name='孙光宪' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='五代词人，益州华阳人，官至门下侍郎兼户部尚书同平章事。词多写艳情，《花间集》收十七首。' WHERE name='欧阳炯' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='五代词人，字平珪，高阳人，仕前蜀至司徒。词收入《花间集》，多咏艳情小令。' WHERE name='毛文锡' AND (intro IS NULL OR intro='');
UPDATE poet SET intro='五代词人，字松卿，陇西人，牛僧孺之后，仕前蜀为给事中。词收入《花间集》，词风纤丽。' WHERE name='牛峤' AND (intro IS NULL OR intro='');
