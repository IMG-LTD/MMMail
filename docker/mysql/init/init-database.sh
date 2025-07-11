#!/bin/bash

# 启动MySQL服务
service mysql start

# 创建数据库和用户
echo "CREATE DATABASE IF NOT EXISTS mmmail DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | mysql -u root -p$MYSQL_ROOT_PASSWORD
echo "CREATE USER IF NOT EXISTS 'mmmail'@'%' IDENTIFIED BY 'mmmail123';" | mysql -u root -p$MYSQL_ROOT_PASSWORD
echo "GRANT ALL ON mmmail.* TO 'mmmail'@'%';" | mysql -u root -p$MYSQL_ROOT_PASSWORD

# 导入初始数据
echo "正在导入初始数据库结构和数据..."
mysql -u root -p$MYSQL_ROOT_PASSWORD mmmail < /docker-entrypoint-initdb.d/mmmail_v3.sql
echo "数据库初始化完成！"
