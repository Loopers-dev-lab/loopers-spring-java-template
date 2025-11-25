-- =====================================================
-- 01. 브랜드 100개 생성
-- =====================================================
-- 설명: 다양한 브랜드를 생성하여 상품이 고르게 분포되도록 합니다.
-- 소요 시간: 약 1초
-- =====================================================

SET @i = 1;

-- 100개 브랜드 생성
INSERT INTO brands (name, description, created_at, updated_at)
SELECT
    CONCAT('브랜드_', LPAD(@i:=@i+1, 3, '0')) AS name,
    CONCAT('브랜드 ', @i, ' 설명입니다.') AS description,
    NOW() AS created_at,
    NOW() AS updated_at
FROM
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 0) AS t1,
    (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 0) AS t2
LIMIT 100;

-- 생성된 브랜드 확인
SELECT COUNT(*) AS total_brands FROM brands;
SELECT * FROM brands ORDER BY id LIMIT 5;
