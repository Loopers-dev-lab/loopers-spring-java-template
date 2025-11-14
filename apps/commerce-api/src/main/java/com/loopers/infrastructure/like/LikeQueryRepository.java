package com.loopers.infrastructure.like;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeQueryRepository {

    private final EntityManager entityManager;

    /**
     * 좋아요 등록 (동시성 안전)
     * INSERT ... ON DUPLICATE KEY UPDATE를 사용하여 원자적으로 처리
     * 
     * @param userId 사용자 ID
     * @param likeTargetId 좋아요 대상 ID
     * @param likeTargetType 좋아요 대상 타입
     * @return 영향받은 행 수 (1: INSERT 또는 UPDATE 성공, 2: UPDATE 성공)
     */
    public long insertOrIgnore(Long userId, Long likeTargetId, String likeTargetType) {

        String sql = """
            INSERT INTO user_like (user_id, like_target_id, like_target_type, created_at, updated_at)
            VALUES (:userId, :likeTargetId, :likeTargetType, NOW(), NOW())
            ON DUPLICATE KEY UPDATE updated_at = NOW()
            """;

        return entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("likeTargetId", likeTargetId)
                .setParameter("likeTargetType", likeTargetType)
                .executeUpdate();
    }

    /**
     * 좋아요 삭제 (동시성 안전)
     * 
     * @param userId 사용자 ID
     * @param likeTargetId 좋아요 대상 ID
     * @param likeTargetType 좋아요 대상 타입
     * @return 영향받은 행 수 (0: 삭제할 데이터 없음, 1: 삭제 성공)
     */
    public long deleteByUserIdAndLikeTargetId(Long userId, Long likeTargetId, String likeTargetType) {
        String sql = """
            DELETE FROM user_like 
            WHERE user_id = :userId 
            AND like_target_id = :likeTargetId 
            AND like_target_type = :likeTargetType
            """;

        return entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("likeTargetId", likeTargetId)
                .setParameter("likeTargetType", likeTargetType)
                .executeUpdate();
    }
}

