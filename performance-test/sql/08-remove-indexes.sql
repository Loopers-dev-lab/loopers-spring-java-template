-- =====================================================
-- 08. AS-IS 테스트를 위한 인덱스 제거
-- =====================================================
-- 성능 비교를 위해 인덱스를 제거합니다
-- 테스트 후 04-create-indexes.sql로 재생성하세요
-- =====================================================

-- 기존 인덱스 제거 (에러 무시)
-- MySQL 8.0에서 IF EXISTS는 특정 버전에서만 지원되므로 프로시저 사용

DROP PROCEDURE IF EXISTS drop_index_if_exists;

DELIMITER //
CREATE PROCEDURE drop_index_if_exists()
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;

    -- products 테이블 인덱스 제거
    DROP INDEX idx_products_brand_id ON products;
    DROP INDEX idx_products_deleted_at ON products;
    DROP INDEX idx_products_brand_deleted ON products;
    DROP INDEX idx_products_created_at ON products;
    DROP INDEX idx_products_price ON products;
    DROP INDEX idx_products_like_count ON products;

    -- likes 테이블 인덱스 제거
    DROP INDEX idx_likes_product_id ON likes;
    DROP INDEX idx_likes_product_deleted ON likes;
    DROP INDEX idx_likes_user_product ON likes;
END//
DELIMITER ;

CALL drop_index_if_exists();
DROP PROCEDURE drop_index_if_exists;

-- 인덱스 제거 확인
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'loopers'
  AND TABLE_NAME IN ('products', 'likes')
  AND INDEX_NAME != 'PRIMARY'
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT 'AS-IS 테스트 준비 완료: 모든 인덱스가 제거되었습니다' AS message;
