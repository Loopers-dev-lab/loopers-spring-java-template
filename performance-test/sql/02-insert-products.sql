-- =====================================================
-- 02. 상품 100,000개 생성
-- =====================================================
-- 설명: 브랜드별로 고르게 분포된 상품을 생성합니다.
-- 소요 시간: 약 1-2분
-- =====================================================

-- 프로시저 생성
DELIMITER $$

DROP PROCEDURE IF EXISTS insert_bulk_products$$

CREATE PROCEDURE insert_bulk_products()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE brand_id_val BIGINT;
    DECLARE brand_count INT;

    -- 브랜드 개수 조회
    SELECT COUNT(*) INTO brand_count FROM brands;

    -- 트랜잭션 시작
    START TRANSACTION;

    WHILE i <= 100000 DO
        -- 브랜드 ID를 순환하며 할당 (1부터 brand_count까지)
        SET brand_id_val = ((i - 1) % brand_count) + 1;

        -- 상품 INSERT
        INSERT INTO products (
            brand_id,
            name,
            price,
            stock,
            description,
            like_count,
            created_at,
            updated_at,
            version
        ) VALUES (
            brand_id_val,
            CONCAT('상품_', LPAD(i, 6, '0')),
            FLOOR(10000 + RAND() * 990000),  -- 10,000 ~ 1,000,000원
            FLOOR(RAND() * 1000),             -- 0 ~ 1000개
            CONCAT('상품 ', i, ' 설명입니다.'),
            0,
            NOW(),
            NOW(),
            0
        );

        SET i = i + 1;

        -- 1000개마다 커밋 (메모리 효율)
        IF i % 1000 = 0 THEN
            COMMIT;
            START TRANSACTION;
        END IF;

        -- 진행률 출력 (10000개마다)
        IF i % 10000 = 0 THEN
            SELECT CONCAT(i, '개 생성 완료...') AS progress;
        END IF;
    END WHILE;

    -- 최종 커밋
    COMMIT;

    SELECT '100,000개 상품 생성 완료!' AS result;
END$$

DELIMITER ;

-- 프로시저 실행
CALL insert_bulk_products();

-- 프로시저 삭제
DROP PROCEDURE IF EXISTS insert_bulk_products;

-- 생성된 상품 확인
SELECT COUNT(*) AS total_products FROM products;
SELECT brand_id, COUNT(*) AS product_count
FROM products
GROUP BY brand_id
ORDER BY brand_id
LIMIT 10;

-- 샘플 데이터 확인
SELECT p.id, b.name AS brand_name, p.name, p.price, p.stock
FROM products p
JOIN brands b ON p.brand_id = b.id
ORDER BY p.id
LIMIT 10;
