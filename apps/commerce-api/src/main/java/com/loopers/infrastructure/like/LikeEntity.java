package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 좋아요 JPA Entity
 * - 복합키 사용 (userId, productId)
 */
@Entity
@Table(name = "likes", indexes = {
        @Index(name = "idx_product_id", columnList = "product_id")
})
@IdClass(LikeEntity.LikeId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LikeEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // === 생성자 ===

    private LikeEntity(String userId, Long productId, LocalDateTime createdAt) {
        this.userId = userId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    // === Domain <-> Entity 변환 ===

    /**
     * Domain 객체로부터 Entity 생성
     */
    public static LikeEntity from(Like like) {
        return new LikeEntity(
                like.getUserId(),
                like.getProductId(),
                like.getCreatedAt()
        );
    }

    /**
     * Entity를 Domain 객체로 변환
     */
    public Like toDomain() {
        return Like.reconstitute(
                this.userId,
                this.productId,
                this.createdAt
        );
    }

    // === 복합키 클래스 ===

    /**
     * 좋아요 복합키 (userId + productId)
     */
    public static class LikeId implements Serializable {
        private String userId;
        private Long productId;

        public LikeId() {
        }

        public LikeId(String userId, Long productId) {
            this.userId = userId;
            this.productId = productId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LikeId likeId = (LikeId) o;
            return Objects.equals(userId, likeId.userId) && Objects.equals(productId, likeId.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, productId);
        }
    }
}
