-- =====================================================
-- 03. 좋아요 500,000개 생성
-- =====================================================
-- 설명: 랜덤하게 좋아요 데이터를 생성합니다.
-- 소요 시간: 약 2-3분
-- =====================================================

-- 프로시저 생성
DELIMITER $$

DROP PROCEDURE IF EXISTS insert_bulk_likes$$

CREATE PROCEDURE insert_bulk_likes()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE product_id_val BIGINT;
    DECLARE user_id_val VARCHAR(10);

    -- 트랜잭션 시작
    START TRANSACTION;

    WHILE i <= 500000 DO
        -- 랜덤 상품 ID (1 ~ 100000)
        SET product_id_val = FLOOR(1 + RAND() * 100000);

        -- 랜덤 사용자 ID (user1 ~ user50000) - VARCHAR(10) 제약 고려
        SET user_id_val = CONCAT('user', FLOOR(1 + RAND() * 50000));

        -- 좋아요 INSERT (중복은 무시)
        INSERT IGNORE INTO likes (
            user_id,
            product_id,
            created_at,
            updated_at
        ) VALUES (
            user_id_val,
            product_id_val,
            NOW(),
            NOW()
        );

        SET i = i + 1;

        -- 1000개마다 커밋
        IF i % 1000 = 0 THEN
            COMMIT;
            START TRANSACTION;
        END IF;

        -- 진행률 출력 (50000개마다)
        IF i % 50000 = 0 THEN
            SELECT CONCAT(i, '개 생성 완료...') AS progress;
        END IF;
    END WHILE;

    -- 최종 커밋
    COMMIT;

    SELECT '500,000개 좋아요 생성 완료!' AS result;
END$$

DELIMITER ;

-- 프로시저 실행
CALL insert_bulk_likes();

-- 프로시저 삭제
DROP PROCEDURE IF EXISTS insert_bulk_likes;

-- 생성된 좋아요 확인
SELECT COUNT(*) AS total_likes FROM likes;

-- 상품별 좋아요 수 상위 10개
SELECT product_id, COUNT(*) AS like_count
FROM likes
WHERE deleted_at IS NULL
GROUP BY product_id
ORDER BY like_count DESC
LIMIT 10;

-- 특정 상품의 좋아요 수 확인
SELECT p.id, p.name, COUNT(l.id) AS like_count
FROM products p
LEFT JOIN likes l ON p.id = l.product_id AND l.deleted_at IS NULL
WHERE p.id IN (1, 100, 1000, 10000, 50000)
GROUP BY p.id, p.name
ORDER BY p.id;
