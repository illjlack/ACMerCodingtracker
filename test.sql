-- 1) 新建所有 test_<洛谷账号> 用户
INSERT INTO `user` (username, password, real_name)
VALUES
  ('test_Reisentyan',     'test123', 'Luogu测试1'),
  ('test_cyanle',         'test123', 'Luogu测试2'),
  ('test_yuanzhiti',      'test123', 'Luogu测试3'),
  ('test_Osakooo',        'test123', 'Luogu测试4'),
  ('test_1101703918',     'test123', 'Luogu测试5'),
  ('test_Jimmdil Uni',    'test123', 'Luogu测试6'),
  ('test_Paramecium-4',   'test123', 'Luogu测试7'),
  ('test_FFFF_',          'test123', 'Luogu测试8'),
  ('test_yyds123',        'test123', 'Luogu测试9'),
  ('test_DangX',          'test123', 'Luogu测试10'),
  ('test_kkzjyy',         'test123', 'Luogu测试11'),
  ('test_fxyyy',          'test123', 'Luogu测试12'),
  ('test_nuoPByan',       'test123', 'Luogu测试13'),
  ('test_fm_los',         'test123', 'Luogu测试14'),
  ('test_LianKirin',      'test123', 'Luogu测试15'),
  ('test_ganzaoji',       'test123', 'Luogu测试16'),
  ('test_Ritzer',         'test123', 'Luogu测试17'),
  ('test_Paris_dream',    'test123', 'Luogu测试18'),
  ('test_zanq',           'test123', 'Luogu测试19'),
  ('test_xhzhaoyibo',     'test123', 'Luogu测试20'),
  ('test_oook_',          'test123', 'Luogu测试21'),
  ('test_yza_sk',         'test123', 'Luogu测试22'),
  ('test_OLLN',           'test123', 'Luogu测试23'),
  ('test_xylalala',       'test123', 'Luogu测试24'),
  ('test_ther-121',       'test123', 'Luogu测试25'),
  ('test_SisySycho',      'test123', 'Luogu测试26'),
  ('test_CFerr',          'test123', 'Luogu测试27'),
  ('test_admin-123',      'test123', 'Luogu测试28'),
  ('test_8letters',       'test123', 'Luogu测试29'),
  ('test_qqqqqa',         'test123', 'Luogu测试30'),
  ('test_wxsQ7',          'test123', 'Luogu测试31'),
  ('test_sgsh1',          'test123', 'Luogu测试32'),
  ('test_xiaoqiwangzhe',  'test123', 'Luogu测试33'),
  ('test_nsym11',         'test123', 'Luogu测试34'),
  ('test_Bulonte',        'test123', 'Luogu测试35'),
  ('test_lihri686',       'test123', 'Luogu测试36'),
  ('test_dz.zz',          'test123', 'Luogu测试37');

-- 2) 将以上用户与洛谷账号关联到 user_oj（去重&不重复插入）
INSERT INTO `user_oj` (user_id, account_name, platform)
SELECT u.id, a.luogu, 'LUOGU'
FROM (
  SELECT 'Reisentyan'    AS luogu UNION
  SELECT 'cyanle'        UNION
  SELECT 'yuanzhiti'     UNION
  SELECT 'Osakooo'       UNION
  SELECT '1101703918'    UNION
  SELECT 'Jimmdil Uni'   UNION
  SELECT 'Paramecium-4'  UNION
  SELECT 'FFFF_'         UNION
  SELECT 'yyds123'       UNION
  SELECT 'DangX'         UNION
  SELECT 'kkzjyy'        UNION
  SELECT 'fxyyy'         UNION
  SELECT 'nuoPByan'      UNION
  SELECT 'fm_los'        UNION
  SELECT 'LianKirin'     UNION
  SELECT 'ganzaoji'      UNION
  SELECT 'Ritzer'        UNION
  SELECT 'Paris_dream'   UNION
  SELECT 'zanq'          UNION
  SELECT 'xhzhaoyibo'    UNION
  SELECT 'oook_'         UNION
  SELECT 'yza_sk'        UNION
  SELECT 'OLLN'          UNION
  SELECT 'xylalala'      UNION
  SELECT 'ther-121'      UNION
  SELECT 'SisySycho'     UNION
  SELECT 'CFerr'         UNION
  SELECT 'admin-123'     UNION
  SELECT '8letters'      UNION
  SELECT 'qqqqqa'        UNION
  SELECT 'wxsQ7'         UNION
  SELECT 'sgsh1'         UNION
  SELECT 'xiaoqiwangzhe' UNION
  SELECT 'nsym11'        UNION
  SELECT 'Bulonte'       UNION
  SELECT 'lihri686'      UNION
  SELECT 'dz.zz'
) AS a
JOIN `user` AS u
  ON u.username = CONCAT('test_', a.luogu)
WHERE NOT EXISTS (
  SELECT 1
  FROM `user_oj` oj
  WHERE oj.user_id       = u.id
    AND oj.platform      = 'LUOGU'
    AND oj.account_name  = a.luogu
);











INSERT INTO user (username, password, real_name)
VALUES
  ('cf_test_fxyyy',        'test123', 'cf测试1'),
  ('cf_test_nuoPByan',     'test123', 'cf测试2'),
  ('cf_test_fm_los',       'test123', 'cf测试3'),
  ('cf_test_LianKirin',    'test123', 'cf测试4'),
  ('cf_test_ganzaoji',     'test123', 'cf测试5'),
  ('cf_test_Ritzer',       'test123', 'cf测试6'),
  ('cf_test_Paris_dream',  'test123', 'cf测试7'),
  ('cf_test_zanq',         'test123', 'cf测试8'),
  ('cf_test_xhzhaoyibo',   'test123', 'cf测试9'),
  ('cf_test_oook_',        'test123', 'cf测试10'),
  ('cf_test_yza_sk',       'test123', 'cf测试11'),
  ('cf_test_OLLN',         'test123', 'cf测试12'),
  ('cf_test_xylalala',     'test123', 'cf测试13'),
  ('cf_test_ther-121',     'test123', 'cf测试14'),
  ('cf_test_SisySycho',    'test123', 'cf测试15'),
  ('cf_test_CFerr',        'test123', 'cf测试16'),
  ('cf_test_admin-123',    'test123', 'cf测试17'),
  ('cf_test_8letters',     'test123', 'cf测试18'),
  ('cf_test_qqqqqa',       'test123', 'cf测试19'),
  ('cf_test_wxsQ7',        'test123', 'cf测试20'),
  ('cf_test_sgsh1',        'test123', 'cf测试21'),
  ('cf_test_xiaoqiwangzhe','test123', 'cf测试22'),
  ('cf_test_nsym11',       'test123', 'cf测试23'),
  ('cf_test_Bulonte',      'test123', 'cf测试24'),
  ('cf_test_lihri686',     'test123', 'cf测试25'),
  ('cf_test_dz.zz',        'test123', 'cf测试26');

INSERT INTO user_oj (user_id, account_name, platform)
SELECT u.id, a.cf, 'CODEFORCES'
FROM (
  SELECT 'fxyyy'       AS cf UNION
  SELECT 'nuoPByan'    UNION
  SELECT 'fm_los'      UNION
  SELECT 'LianKirin'   UNION
  SELECT 'ganzaoji'    UNION
  SELECT 'Ritzer'      UNION
  SELECT 'Paris_dream' UNION
  SELECT 'zanq'        UNION
  SELECT 'xhzhaoyibo'  UNION
  SELECT 'oook_'       UNION
  SELECT 'yza_sk'      UNION
  SELECT 'OLLN'        UNION
  SELECT 'xylalala'    UNION
  SELECT 'ther-121'    UNION
  SELECT 'SisySycho'   UNION
  SELECT 'CFerr'       UNION
  SELECT 'admin-123'   UNION
  SELECT '8letters'    UNION
  SELECT 'qqqqqa'      UNION
  SELECT 'wxsQ7'       UNION
  SELECT 'sgsh1'       UNION
  SELECT 'xiaoqiwangzhe' UNION
  SELECT 'nsym11'      UNION
  SELECT 'Bulonte'     UNION
  SELECT 'lihri686'    UNION
  SELECT 'dz.zz'
) AS a
JOIN user AS u
  ON u.username = CONCAT('cf_test_', a.cf)
WHERE NOT EXISTS (
  SELECT 1
  FROM user_oj oj
  WHERE oj.user_id       = u.id
    AND oj.platform      = 'CODEFORCES'
    AND oj.account_name  = a.cf
);


-- 插入管理员用户
INSERT INTO `user` (
    username,
    password,
    real_name,
    email,
    major,
    avatar
) VALUES (
    'admin',
    '$2a$10$gP8PAqILdbQPmElGIcOm9upK3btABe9xabN.uENsKjr0IKW1tjxxO', -- 123456的加密
    '系统管理员',
    '3379782451@qq.com',
    '计算机',
    NULL
);

-- 获取该用户的 ID（假设是自增主键）
-- 插入角色信息
INSERT INTO `user_roles` (
    user_id,
    roles
) SELECT id, 'ADMIN'
  FROM `user` WHERE username = 'admin';
