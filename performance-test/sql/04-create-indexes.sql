-- =====================================================
-- 04. 인덱스 생성
-- =====================================================
-- 설명: 조회 성능 개선을 위한 인덱스를 생성합니다.
-- =====================================================

-- 인덱스 생성 전 상태 확인
SHOW INDEXES FROM products;
SHOW INDEXES FROM likes;

-- =====================================================
-- Products 테이블 인덱스
-- =====================================================

-- 1. 브랜드 필터링용 인덱스
-- 용도: WHERE brand_id = ? 조건 최적화
CREATE INDEX idx_products_brand_id ON products(brand_id);

-- 2. Soft Delete 조회용 인덱스
-- 용도: WHERE deleted_at IS NULL 조건 최적화
CREATE INDEX idx_products_deleted_at ON products(deleted_at);

-- 3. 브랜드 + Soft Delete 복합 인덱스
-- 용도: WHERE brand_id = ? AND deleted_at IS NULL 최적화
CREATE INDEX idx_products_brand_deleted ON products(brand_id, deleted_at);

-- 4. 생성일 정렬용 인덱스
-- 용도: ORDER BY created_at DESC 최적화
CREATE INDEX idx_products_created_at ON products(created_at DESC);

-- 5. 가격 정렬용 인덱스
-- 용도: ORDER BY price ASC 최적화
CREATE INDEX idx_products_price ON products(price);

-- =====================================================
-- Likes 테이블 인덱스
-- =====================================================

-- 1. 상품별 좋아요 조회용 인덱스
-- 용도: WHERE product_id = ? 조건 최적화
CREATE INDEX idx_likes_product_id ON likes(product_id);

-- 2. 상품 + Soft Delete 복합 인덱스
-- 용도: WHERE product_id = ? AND deleted_at IS NULL 최적화
CREATE INDEX idx_likes_product_deleted ON likes(product_id, deleted_at);

-- 3. 사용자 + 상품 복합 인덱스
-- 용도: WHERE user_id = ? AND product_id = ? 중복 체크 최적화
-- 참고: 이미 UNIQUE 제약조건이 있다면 불필요할 수 있음
CREATE INDEX idx_likes_user_product ON likes(user_id, product_id);

-- =====================================================
-- 인덱스 생성 완료 확인
-- =====================================================

SELECT '인덱스 생성 완료!' AS result;

-- Products 테이블 인덱스 확인
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'loopers'
  AND TABLE_NAME = 'products'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- Likes 테이블 인덱스 확인
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'loopers'
  AND TABLE_NAME = 'likes'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- =====================================================
-- 인덱스 삭제 (필요시)
-- =====================================================
-- DROP INDEX idx_products_brand_id ON products;
-- DROP INDEX idx_products_deleted_at ON products;
-- DROP INDEX idx_products_brand_deleted ON products;
-- DROP INDEX idx_products_created_at ON products;
-- DROP INDEX idx_products_price ON products;
-- DROP INDEX idx_likes_product_id ON likes;
-- DROP INDEX idx_likes_product_deleted ON likes;
-- DROP INDEX idx_likes_user_product ON likes;
