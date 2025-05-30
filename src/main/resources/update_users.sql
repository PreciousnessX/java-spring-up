-- 更新用户表结构
ALTER TABLE `users` 
    MODIFY COLUMN `username` varchar(50) NOT NULL,
    MODIFY COLUMN `password` varchar(100) NOT NULL,
    MODIFY COLUMN `email` varchar(100) NOT NULL,
    MODIFY COLUMN `phone_number` varchar(20),
    MODIFY COLUMN `created_at` datetime,
    MODIFY COLUMN `updated_at` datetime;
