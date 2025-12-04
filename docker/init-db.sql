-- PG Simulator를 위한 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS paymentgateway CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
GRANT ALL PRIVILEGES ON paymentgateway.* TO 'application'@'%';
FLUSH PRIVILEGES;
