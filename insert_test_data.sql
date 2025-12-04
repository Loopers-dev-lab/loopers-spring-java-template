-- ============================================================================
-- 성능 테스트를 위한 대량 데이터 삽입 스크립트
-- ============================================================================
-- 
-- 이 스크립트는 다음을 생성합니다:
-- 1. 100개의 브랜드 데이터
-- 2. 100,000개의 상품 데이터
--
-- 데이터 분포:
-- - brand_id: 1~100 사이 균등 분포 (각 브랜드당 약 1,000개)
-- - price: 1,000원 ~ 1,000,000원 (저가 10%, 중저가 30%, 중가 40%, 고가 15%, 프리미엄 5%)
-- - like_count: 0 ~ 100,000 (다양한 분포)
-- - stock: 0 ~ 10,000 (품절 5%, 다양한 재고 수준)
-- - is_deleted: false 95%, true 5%
-- - created_at: 과거 1년간 분포 (최근 데이터가 더 많음)
--
-- ============================================================================

-- 1. 브랜드 데이터 생성 (100개)
-- 다양한 브랜드를 생성하여 brandId 필터링 테스트에 활용
INSERT INTO brands (name, description, created_at, updated_at, deleted_at)
SELECT 
    CONCAT('Brand_', LPAD(n, 3, '0')) as name,
    CONCAT('Description for Brand ', n) as description,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as updated_at,
    NULL as deleted_at
FROM (
    SELECT @row := @row + 1 as n
    FROM 
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
        (SELECT @row := 0) r
    LIMIT 100
) numbers;

-- 2. 상품 데이터 생성 (100,000개)
-- 각 컬럼의 값이 다양하게 분포하도록 설정
INSERT INTO products (
    name, 
    brand_id, 
    price, 
    like_count, 
    stock, 
    version, 
    is_deleted, 
    created_at, 
    updated_at, 
    deleted_at
)
SELECT 
    CONCAT('Product_', LPAD(n, 6, '0')) as name,
    -- brandId: 1~100 사이 균등 분포 (각 브랜드당 약 1,000개씩)
    ((n - 1) % 100) + 1 as brand_id,
    -- price: 1,000원 ~ 1,000,000원 사이 다양하게 분포
    -- 가격대별로 분포: 저가(10%), 중저가(30%), 중가(40%), 고가(15%), 프리미엄(5%)
    -- n % 100을 사용하여 균등 분포 보장
    CASE 
        WHEN n % 100 < 10 THEN FLOOR(1000 + (n * 7) % 9000)                    -- 1,000 ~ 10,000 (10%)
        WHEN n % 100 < 40 THEN FLOOR(10000 + (n * 11) % 40000)                  -- 10,000 ~ 50,000 (30%)
        WHEN n % 100 < 80 THEN FLOOR(50000 + (n * 13) % 200000)                 -- 50,000 ~ 250,000 (40%)
        WHEN n % 100 < 95 THEN FLOOR(250000 + (n * 17) % 500000)                -- 250,000 ~ 750,000 (15%)
        ELSE FLOOR(750000 + (n * 19) % 250000)                                   -- 750,000 ~ 1,000,000 (5%)
    END as price,
    -- like_count: 0 ~ 100,000 사이 다양하게 분포
    -- 좋아요 수는 정규분포와 유사하게, 대부분은 중간값에 집중
    CASE 
        WHEN n % 100 < 20 THEN (n * 3) % 100                            -- 0 ~ 100 (20%)
        WHEN n % 100 < 50 THEN 100 + (n * 5) % 900                      -- 100 ~ 1,000 (30%)
        WHEN n % 100 < 80 THEN 1000 + (n * 7) % 9000                    -- 1,000 ~ 10,000 (30%)
        WHEN n % 100 < 95 THEN 10000 + (n * 11) % 40000                  -- 10,000 ~ 50,000 (15%)
        ELSE 50000 + (n * 13) % 50000                                     -- 50,000 ~ 100,000 (5%)
    END as like_count,
    -- stock: 0 ~ 10,000 사이 다양하게 분포
    -- 재고는 대부분 양수, 일부는 0 (품절)
    CASE 
        WHEN n % 100 < 5 THEN 0                                              -- 품절 (5%)
        WHEN n % 100 < 30 THEN 1 + (n * 2) % 99                          -- 1 ~ 100 (25%)
        WHEN n % 100 < 70 THEN 100 + (n * 3) % 900                      -- 100 ~ 1,000 (40%)
        WHEN n % 100 < 95 THEN 1000 + (n * 5) % 4000                    -- 1,000 ~ 5,000 (25%)
        ELSE 5000 + (n * 7) % 5000                                       -- 5,000 ~ 10,000 (5%)
    END as stock,
    0 as version,
    -- is_deleted: 대부분 false, 약 5%는 true
    CASE WHEN n % 100 < 5 THEN 1 ELSE 0 END as is_deleted,
    -- created_at: 과거 1년간 분포 (최신순 정렬 테스트를 위해)
    -- 최근 데이터가 더 많도록 분포 (최근 3개월: 50%, 3~6개월: 30%, 6~12개월: 20%)
    CASE 
        WHEN n % 100 < 50 THEN DATE_SUB(NOW(), INTERVAL (n * 2) % 90 DAY)      -- 최근 3개월 (50%)
        WHEN n % 100 < 80 THEN DATE_SUB(NOW(), INTERVAL (90 + (n * 3) % 90) DAY) -- 3~6개월 (30%)
        ELSE DATE_SUB(NOW(), INTERVAL (180 + (n * 5) % 185) DAY)                  -- 6~12개월 (20%)
    END as created_at,
    DATE_SUB(NOW(), INTERVAL (n * 7) % 365 DAY) as updated_at,
    -- deleted_at: is_deleted가 true인 경우에만 설정
    CASE 
        WHEN n % 100 < 5 THEN DATE_SUB(NOW(), INTERVAL (n * 11) % 30 DAY)
        ELSE NULL
    END as deleted_at
FROM (
    SELECT @row := @row + 1 as n
    FROM 
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
        (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t5,
        (SELECT @row := 0) r
    LIMIT 100000
) numbers;

-- 3. 인덱스 확인 및 생성 (성능 최적화)
-- brand_id, is_deleted, created_at, price, like_count에 대한 인덱스가 있는지 확인
-- 필요시 다음 인덱스들을 생성 (이미 존재하면 에러 발생하므로 주의)

-- brand_id와 is_deleted 복합 인덱스 (브랜드 필터링 + 삭제 여부 필터링)
-- CREATE INDEX idx_products_brand_deleted ON products(brand_id, is_deleted);

-- created_at 인덱스 (최신순 정렬)
-- CREATE INDEX idx_products_created_at ON products(created_at DESC);

-- price 인덱스 (가격 정렬)
-- CREATE INDEX idx_products_price ON products(price);

-- like_count 인덱스 (좋아요 정렬)
-- CREATE INDEX idx_products_like_count ON products(like_count DESC);

-- brand_id, is_deleted, created_at 복합 인덱스 (최신순 조회 최적화)
-- CREATE INDEX idx_products_brand_deleted_created ON products(brand_id, is_deleted, created_at DESC);

-- brand_id, is_deleted, price 복합 인덱스 (가격순 조회 최적화)
-- CREATE INDEX idx_products_brand_deleted_price ON products(brand_id, is_deleted, price);

-- brand_id, is_deleted, like_count 복합 인덱스 (좋아요순 조회 최적화)
-- CREATE INDEX idx_products_brand_deleted_likes ON products(brand_id, is_deleted, like_count DESC);

-- 4. 데이터 검증 쿼리
-- 삽입된 데이터 확인

-- 전체 상품 수 확인
SELECT COUNT(*) as total_products FROM products;

-- is_deleted별 상품 수 확인
SELECT is_deleted, COUNT(*) as count 
FROM products 
GROUP BY is_deleted;

-- 브랜드별 상품 수 확인 (상위 10개)
SELECT brand_id, COUNT(*) as product_count 
FROM products 
WHERE is_deleted = 0 
GROUP BY brand_id 
ORDER BY product_count DESC 
LIMIT 10;

-- 가격 분포 확인
SELECT 
    CASE 
        WHEN price < 10000 THEN '1만원 미만'
        WHEN price < 50000 THEN '1만원~5만원'
        WHEN price < 250000 THEN '5만원~25만원'
        WHEN price < 750000 THEN '25만원~75만원'
        ELSE '75만원 이상'
    END as price_range,
    COUNT(*) as count
FROM products 
WHERE is_deleted = 0
GROUP BY price_range
ORDER BY MIN(price);

-- 좋아요 수 분포 확인
SELECT 
    CASE 
        WHEN like_count < 100 THEN '0~100'
        WHEN like_count < 1000 THEN '100~1,000'
        WHEN like_count < 10000 THEN '1,000~10,000'
        WHEN like_count < 50000 THEN '10,000~50,000'
        ELSE '50,000 이상'
    END as like_range,
    COUNT(*) as count
FROM products 
WHERE is_deleted = 0
GROUP BY like_range
ORDER BY MIN(like_count);

-- 최신순 정렬 테스트 (상위 10개)
SELECT id, name, brand_id, price, like_count, created_at 
FROM products 
WHERE brand_id = 1 AND is_deleted = 0 
ORDER BY created_at DESC 
LIMIT 10;

-- 가격 오름차순 정렬 테스트 (상위 10개)
SELECT id, name, brand_id, price, like_count 
FROM products 
WHERE brand_id = 1 AND is_deleted = 0 
ORDER BY price ASC 
LIMIT 10;

-- 좋아요 내림차순 정렬 테스트 (상위 10개)
SELECT id, name, brand_id, price, like_count 
FROM products 
WHERE brand_id = 1 AND is_deleted = 0 
ORDER BY like_count DESC 
LIMIT 10;

