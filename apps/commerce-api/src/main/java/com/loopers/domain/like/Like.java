package com.loopers.domain.like;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 좋아요 도메인 모델
 * - 순수 도메인 객체 (JPA 의존성 없음)
 * - 유저와 상품 간의 관계를 나타내는 도메인
 * - 복합키: userId + productId
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Like {

    private String userId;
    private Long productId;
    private LocalDateTime createdAt;

    /**
     * 좋아요 생성 팩토리 메서드
     */
    public static Like create(String userId, Long productId) {
        validateUserId(userId);
        validateProductId(productId);
        return new Like(userId, productId, LocalDateTime.now());
    }

    /**
     * 재구성 팩토리 메서드 (Infrastructure에서 사용)
     */
    public static Like reconstitute(
            String userId,
            Long productId,
            LocalDateTime createdAt
    ) {
        return new Like(userId, productId, createdAt);
    }

    /**
     * 동일 사용자 확인
     */
    public boolean isSameUser(String userId) {
        return this.userId.equals(userId);
    }

    /**
     * 동일 상품 확인
     */
    public boolean isSameProduct(Long productId) {
        return this.productId.equals(productId);
    }

    // === Validation Methods ===

    private static void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다");
        }
    }
}
