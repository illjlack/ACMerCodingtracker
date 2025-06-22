CREATE DATABASE IF NOT EXISTS codingtracker DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE codingtracker;

-- 修改extoj_pb_info表的pid字段长度，从50增加到200
ALTER TABLE extoj_pb_info MODIFY COLUMN pid VARCHAR(200) NOT NULL;
