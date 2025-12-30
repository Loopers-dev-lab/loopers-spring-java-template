-- =====================================================
-- k6 부하 테스트용 시드 데이터 생성 스크립트 (수정 버전)
-- =====================================================
-- 이 스크립트는 k6 부하 테스트를 위해 필요한 최소 데이터를 생성합니다.
-- 
-- 생성 범위:
-- - User: ID 1~10
-- - Product: ID 1~1000
-- - Brand: ID 1~100 (Product와 연결)
-- - Stock: Product 1~1000 각각 (충분한 재고)
-- - Point: User 1~10 각각
-- - Coupon: 각 User마다 FIXED_AMOUNT, PERCENTAGE 쿠폰 각 1개
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
-- INSERT IGNORE 사용으로 중복 시 무시
INSERT IGNORE INTO `brand` (`id`, `name`, `description`, `status`, `is_visible`, `is_sellable`, `created_at`, `updated_at`, `deleted_at`)
WITH RECURSIVE brand_sequence AS (
    SELECT 1 AS brand_id
    UNION ALL
    SELECT brand_id + 1
    FROM brand_sequence
    WHERE brand_id < 100
)
SELECT 
    brand_id,
    CONCAT('Test Brand ', LPAD(brand_id, 3, '0')),
    CONCAT('Test Brand Description ', brand_id),
    'ON_SALE',
    1,
    1,
    NOW(),
    NOW(),
    NULL
FROM brand_sequence;

-- 4. Product 데이터 삽입 (ID: 1~1000)
INSERT IGNORE INTO `product` (`id`, `brand_id`, `name`, `description`, `price`, `status`, `is_visible`, `is_sellable`, `created_at`, `updated_at`, `deleted_at`)
WITH RECURSIVE product_sequence AS (
    SELECT 1 AS product_id
    UNION ALL
    SELECT product_id + 1
    FROM product_sequence
    WHERE product_id < 1000
)
SELECT 
    product_id,
    ((product_id - 1) % 100) + 1 AS brand_id,
    CONCAT('Test Product ', LPAD(product_id, 4, '0')),
    CONCAT('Test Product Description ', product_id),
    10000 + (product_id * 100) AS price,
    'ON_SALE',
    1,
    1,
    NOW(),
    NOW(),
    NULL
FROM product_sequence;

-- 5. Stock 데이터 삽입 (product_id: 1~1000)
INSERT IGNORE INTO `stock` (`id`, `product_id`, `quantity`, `created_at`, `updated_at`, `deleted_at`)
WITH RECURSIVE stock_sequence AS (
    SELECT 1 AS product_id
    UNION ALL
    SELECT product_id + 1
    FROM stock_sequence
    WHERE product_id < 1000
)
SELECT 
    product_id AS id,
    product_id AS product_id,
    10000000 AS quantity,
    NOW(),
    NOW(),
    NULL
FROM stock_sequence;

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

-- =====================================================
-- 데이터 생성 완료
-- =====================================================

