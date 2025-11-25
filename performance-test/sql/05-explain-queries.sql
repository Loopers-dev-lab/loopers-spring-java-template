-- =====================================================
-- 05. EXPLAIN 쿼리 분석
-- =====================================================
-- 설명: 실제 사용되는 쿼리의 실행 계획을 분석합니다.
-- 사용법: 인덱스 생성 전/후에 각각 실행하여 비교
-- =====================================================

SELECT '======================================' AS separator;
SELECT '1. 브랜드 필터 + 좋아요 순 정렬 쿼리' AS test_name;
SELECT '======================================' AS separator;

-- 실제 ProductJpaRepository에서 사용되는 쿼리
EXPLAIN ANALYZE
SELECT p.*
FROM products p
LEFT JOIN likes l ON p.id = l.product_id AND l.deleted_at IS NULL
WHERE p.brand_id = 1 AND p.deleted_at IS NULL
GROUP BY p.id
ORDER BY COUNT(l.id) DESC
LIMIT 20;

SELECT '' AS empty_line;
SELECT '======================================' AS separator;
SELECT '2. 전체 상품 좋아요 순 정렬 쿼리' AS test_name;
SELECT '======================================' AS separator;

EXPLAIN ANALYZE
SELECT p.*
FROM products p
LEFT JOIN likes l ON p.id = l.product_id AND l.deleted_at IS NULL
WHERE p.deleted_at IS NULL
GROUP BY p.id
ORDER BY COUNT(l.id) DESC
LIMIT 20;

SELECT '' AS empty_line;
SELECT '======================================' AS separator;
SELECT '3. 브랜드 필터 + 가격 순 정렬 쿼리' AS test_name;
SELECT '======================================' AS separator;

EXPLAIN ANALYZE
SELECT p.*
FROM products p
WHERE p.brand_id = 1 AND p.deleted_at IS NULL
ORDER BY p.price ASC
LIMIT 20;

SELECT '' AS empty_line;
SELECT '======================================' AS separator;
SELECT '4. 브랜드 필터 + 최신순 정렬 쿼리' AS test_name;
SELECT '======================================' AS separator;

EXPLAIN ANALYZE
SELECT p.*
FROM products p
WHERE p.brand_id = 1 AND p.deleted_at IS NULL
ORDER BY p.created_at DESC
LIMIT 20;

SELECT '' AS empty_line;
SELECT '======================================' AS separator;
SELECT '5. 특정 상품의 좋아요 수 조회' AS test_name;
SELECT '======================================' AS separator;

EXPLAIN ANALYZE
SELECT COUNT(*)
FROM likes
WHERE product_id = 100 AND deleted_at IS NULL;

SELECT '' AS empty_line;
SELECT '======================================' AS separator;
SELECT '실제 쿼리 실행 시간 측정' AS test_name;
SELECT '======================================' AS separator;

-- 시간 측정 시작
SET @start_time = NOW(6);

-- 브랜드 필터 + 좋아요 순 정렬 실행
SELECT p.id, p.name, COUNT(l.id) AS like_count
FROM products p
LEFT JOIN likes l ON p.id = l.product_id AND l.deleted_at IS NULL
WHERE p.brand_id = 1 AND p.deleted_at IS NULL
GROUP BY p.id, p.name
ORDER BY like_count DESC
LIMIT 20;

-- 시간 측정 종료
SET @end_time = NOW(6);

-- 실행 시간 출력
SELECT
    TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 1000 AS execution_time_ms,
    '브랜드 필터 + 좋아요 순 정렬 쿼리 실행 시간 (밀리초)' AS description;

SELECT '' AS empty_line;
SELECT '======================================' AS separator;
SELECT '테이블 통계 정보' AS test_name;
SELECT '======================================' AS separator;

-- 테이블별 레코드 수
SELECT 'products' AS table_name, COUNT(*) AS record_count FROM products
UNION ALL
SELECT 'brands' AS table_name, COUNT(*) AS record_count FROM brands
UNION ALL
SELECT 'likes' AS table_name, COUNT(*) AS record_count FROM likes;

-- 브랜드별 상품 분포
SELECT
    b.id AS brand_id,
    b.name AS brand_name,
    COUNT(p.id) AS product_count,
    COUNT(l.id) AS total_likes
FROM brands b
LEFT JOIN products p ON b.id = p.brand_id AND p.deleted_at IS NULL
LEFT JOIN likes l ON p.id = l.product_id AND l.deleted_at IS NULL
GROUP BY b.id, b.name
ORDER BY b.id
LIMIT 10;
