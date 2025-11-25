-- =====================================================
-- 06. likeCount 마이그레이션
-- =====================================================
-- 설명: 기존 데이터의 likeCount를 실제 좋아요 수로 초기화
-- 실행 시점: Product 엔티티에 likeCount 컬럼 추가 후
-- =====================================================

-- 1. likeCount 컬럼이 존재하는지 확인
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'loopers'
  AND TABLE_NAME = 'products'
  AND COLUMN_NAME = 'like_count';

-- 2. likeCount 초기화 (기존 데이터가 있는 경우)
UPDATE products p
SET p.like_count = (
    SELECT COUNT(*)
    FROM likes l
    WHERE l.product_id = p.id
      AND l.deleted_at IS NULL
)
WHERE p.deleted_at IS NULL;

-- 3. 초기화 결과 확인
SELECT
    p.id,
    p.name,
    p.like_count,
    (SELECT COUNT(*) FROM likes l WHERE l.product_id = p.id AND l.deleted_at IS NULL) AS actual_like_count
FROM products p
WHERE p.deleted_at IS NULL
ORDER BY p.like_count DESC
LIMIT 20;

-- 4. likeCount와 실제 좋아요 수가 일치하는지 검증
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN '✅ 모든 상품의 likeCount가 정확합니다!'
        ELSE CONCAT('❌ ', COUNT(*), '개 상품의 likeCount가 불일치합니다.')
    END AS validation_result
FROM (
    SELECT p.id
    FROM products p
    WHERE p.deleted_at IS NULL
      AND p.like_count != (
          SELECT COUNT(*)
          FROM likes l
          WHERE l.product_id = p.id AND l.deleted_at IS NULL
      )
) AS mismatched;

SELECT '마이그레이션 완료!' AS result;
