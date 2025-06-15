-- 插入用户数据
INSERT INTO User (username, password, realName, major, email, avatar, active) VALUES
('admin', '$2a$10$X/hX4Jz9X9X9X9X9X9X9X.9X9X9X9X9X9X9X9X9X9X9X9X9X9X', '管理员', '计算机科学与技术', 'admin@example.com', 'https://example.com/avatar1.jpg', true),
('user1', '$2a$10$X/hX4Jz9X9X9X9X9X9X9X.9X9X9X9X9X9X9X9X9X9X9X9X9X9X', '张三', '软件工程', 'user1@example.com', 'https://example.com/avatar2.jpg', true),
('user2', '$2a$10$X/hX4Jz9X9X9X9X9X9X9X.9X9X9X9X9X9X9X9X9X9X9X9X9X9X', '李四', '人工智能', 'user2@example.com', 'https://example.com/avatar3.jpg', true),
('user3', '$2a$10$X/hX4Jz9X9X9X9X9X9X9X.9X9X9X9X9X9X9X9X9X9X9X9X9X9X', '王五', '数据科学', 'user3@example.com', 'https://example.com/avatar4.jpg', true);

-- 插入用户角色
INSERT INTO user_roles (user_id, roles) VALUES
(1, 'ADMIN'),
(2, 'ACMER'),
(3, 'ACMER'),
(4, 'NEW'); 