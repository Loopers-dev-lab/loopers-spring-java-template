-- =====================================================
-- k6 부하 테스트용 시드 데이터 생성 스크립트 (MySQL 5.7 호환)
-- =====================================================
-- MySQL 8.0 미만 버전에서 사용하는 스크립트입니다.
-- MySQL 8.0 이상을 사용하는 경우 seed-data.sql을 사용하세요.
-- =====================================================

-- 1. User 데이터 삽입 (ID: 1~10)
INSERT INTO `user` (`id`, `login_id`, `email`, `birthday`, `gender`, `created_at`, `updated_at`, `deleted_at`)
VALUES 
    (1, 'user0001', 'user0001@test.com', '1990-01-01', 'MALE', NOW(), NOW(), NULL),
    (2, 'user0002', 'user0002@test.com', '1990-02-01', 'FEMALE', NOW(), NOW(), NULL),
    (3, 'user0003', 'user0003@test.com', '1990-03-01', 'MALE', NOW(), NOW(), NULL),
    (4, 'user0004', 'user0004@test.com', '1990-04-01', 'FEMALE', NOW(), NOW(), NULL),
    (5, 'user0005', 'user0005@test.com', '1990-05-01', 'MALE', NOW(), NOW(), NULL),
    (6, 'user0006', 'user0006@test.com', '1990-06-01', 'FEMALE', NOW(), NOW(), NULL),
    (7, 'user0007', 'user0007@test.com', '1990-07-01', 'MALE', NOW(), NOW(), NULL),
    (8, 'user0008', 'user0008@test.com', '1990-08-01', 'FEMALE', NOW(), NOW(), NULL),
    (9, 'user0009', 'user0009@test.com', '1990-09-01', 'MALE', NOW(), NOW(), NULL),
    (10, 'user0010', 'user0010@test.com', '1990-10-01', 'FEMALE', NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE `login_id` = VALUES(`login_id`);

-- 2. Point 데이터 삽입 (user_id: 1~10)
INSERT INTO `point` (`id`, `user_id`, `amount`, `created_at`, `updated_at`, `deleted_at`)
VALUES 
    (1, 1, 1000000, NOW(), NOW(), NULL),
    (2, 2, 1000000, NOW(), NOW(), NULL),
    (3, 3, 1000000, NOW(), NOW(), NULL),
    (4, 4, 1000000, NOW(), NOW(), NULL),
    (5, 5, 1000000, NOW(), NOW(), NULL),
    (6, 6, 1000000, NOW(), NOW(), NULL),
    (7, 7, 1000000, NOW(), NOW(), NULL),
    (8, 8, 1000000, NOW(), NOW(), NULL),
    (9, 9, 1000000, NOW(), NOW(), NULL),
    (10, 10, 1000000, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

-- 3. Brand 데이터 삽입 (ID: 1~100)
-- MySQL 5.7 호환: 임시 테이블 사용
CREATE TEMPORARY TABLE IF NOT EXISTS temp_brand_ids (id INT AUTO_INCREMENT PRIMARY KEY);
INSERT INTO temp_brand_ids VALUES (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL),
                                  (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL), (NULL);

INSERT INTO `brand` (`id`, `name`, `description`, `status`, `is_visible`, `is_sellable`, `created_at`, `updated_at`, `deleted_at`)
SELECT 
    id,
    CONCAT('Test Brand ', LPAD(id, 3, '0')),
    CONCAT('Test Brand Description ', id),
    'ON_SALE',
    1,
    1,
    NOW(),
    NOW(),
    NULL
FROM temp_brand_ids
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

DROP TEMPORARY TABLE temp_brand_ids;

-- 4. Product 데이터 삽입 (ID: 1~1000)
-- MySQL 5.7 호환: 임시 테이블 사용 (더 큰 임시 테이블 필요)
CREATE TEMPORARY TABLE IF NOT EXISTS temp_product_ids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    idx INT
);

-- 1000개의 ID 생성 (INSERT를 여러 번 반복)
INSERT INTO temp_product_ids (idx) 
SELECT (@row := @row + 1) AS idx
FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
      SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
      SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
      SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t3,
     (SELECT @row := 0) r
LIMIT 1000;

INSERT INTO `product` (`id`, `brand_id`, `name`, `description`, `price`, `status`, `is_visible`, `is_sellable`, `created_at`, `updated_at`, `deleted_at`)
SELECT 
    idx AS id,
    ((idx - 1) % 100) + 1 AS brand_id,
    CONCAT('Test Product ', LPAD(idx, 4, '0')),
    CONCAT('Test Product Description ', idx),
    10000 + (idx * 100) AS price,
    'ON_SALE',
    1,
    1,
    NOW(),
    NOW(),
    NULL
FROM temp_product_ids
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

DROP TEMPORARY TABLE temp_product_ids;

-- 5. Stock 데이터 삽입 (product_id: 1~1000)
CREATE TEMPORARY TABLE IF NOT EXISTS temp_stock_ids (
    idx INT
);

INSERT INTO temp_stock_ids (idx) 
SELECT (@row := @row + 1) AS idx
FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
      SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
      SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL
      SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t3,
     (SELECT @row := 0) r
LIMIT 1000;

INSERT INTO `stock` (`id`, `product_id`, `quantity`, `created_at`, `updated_at`, `deleted_at`)
SELECT 
    idx AS id,
    idx AS product_id,
    10000000 AS quantity,
    NOW(),
    NOW(),
    NULL
FROM temp_stock_ids
ON DUPLICATE KEY UPDATE `quantity` = VALUES(`quantity`);

DROP TEMPORARY TABLE temp_stock_ids;

-- 6. Coupon 데이터 삽입
INSERT INTO `coupon` (`id`, `coupon_type`, `discount_value`, `is_used`, `user_id`, `order_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES 
    (1, 'FIXED_AMOUNT', 5000, 0, 1, NULL, NOW(), NOW(), NULL),
    (2, 'PERCENTAGE', 10, 0, 1, NULL, NOW(), NOW(), NULL),
    (3, 'FIXED_AMOUNT', 5000, 0, 2, NULL, NOW(), NOW(), NULL),
    (4, 'PERCENTAGE', 10, 0, 2, NULL, NOW(), NOW(), NULL),
    (5, 'FIXED_AMOUNT', 5000, 0, 3, NULL, NOW(), NOW(), NULL),
    (6, 'PERCENTAGE', 10, 0, 3, NULL, NOW(), NOW(), NULL),
    (7, 'FIXED_AMOUNT', 5000, 0, 4, NULL, NOW(), NOW(), NULL),
    (8, 'PERCENTAGE', 10, 0, 4, NULL, NOW(), NOW(), NULL),
    (9, 'FIXED_AMOUNT', 5000, 0, 5, NULL, NOW(), NOW(), NULL),
    (10, 'PERCENTAGE', 10, 0, 5, NULL, NOW(), NOW(), NULL),
    (11, 'FIXED_AMOUNT', 5000, 0, 6, NULL, NOW(), NOW(), NULL),
    (12, 'PERCENTAGE', 10, 0, 6, NULL, NOW(), NOW(), NULL),
    (13, 'FIXED_AMOUNT', 5000, 0, 7, NULL, NOW(), NOW(), NULL),
    (14, 'PERCENTAGE', 10, 0, 7, NULL, NOW(), NOW(), NULL),
    (15, 'FIXED_AMOUNT', 5000, 0, 8, NULL, NOW(), NOW(), NULL),
    (16, 'PERCENTAGE', 10, 0, 8, NULL, NOW(), NOW(), NULL),
    (17, 'FIXED_AMOUNT', 5000, 0, 9, NULL, NOW(), NOW(), NULL),
    (18, 'PERCENTAGE', 10, 0, 9, NULL, NOW(), NOW(), NULL),
    (19, 'FIXED_AMOUNT', 5000, 0, 10, NULL, NOW(), NOW(), NULL),
    (20, 'PERCENTAGE', 10, 0, 10, NULL, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE `user_id` = VALUES(`user_id`);

