package com.loopers.domain.like;

import static java.util.Objects.requireNonNull;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.like.event.LikeChangedEvent;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
@Entity
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(name = "uc_likee_user_product", columnNames = {"userId", "productId"})
})
@Getter
@NoArgsConstructor
public class LikeEntity extends BaseEntity<LikeEntity> {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    private LikeEntity(Long userId, Long productId) {
        requireNonNull(userId, "user ID는 필수입니다.");
        requireNonNull(productId, "상품명은 필수입니다.");

        this.userId = userId;
        this.productId = productId;
    }

    public static LikeEntity createEntity(Long userId, Long productId) {
        return new LikeEntity(userId, productId);
    }

    /**
     * 좋아요 생성 시 도메인 이벤트 발행
     * <p>
     * 좋아요 엔티티를 생성하고 좋아요 증가 이벤트를 발행합니다.
     *
     * @param userId    사용자 ID
     * @param productId 상품 ID
     * @return 생성된 좋아요 엔티티
     */
    public static LikeEntity createWithEvent(Long userId, Long productId) {
        LikeEntity likeEntity = new LikeEntity(userId, productId);

        // 도메인 이벤트 발행 (좋아요 증가)
        likeEntity.registerEvent(new LikeChangedEvent(
                productId,
                userId,
                LikeChangedEvent.LikeAction.LIKE,
                +1
        ));

        return likeEntity;
    }

    /**
     * 좋아요 복원 시 도메인 이벤트 발행
     * <p>
     * 소프트 삭제된 좋아요를 복원하고 좋아요 증가 이벤트를 발행합니다.
     */
    public void restoreWithEvent() {
        if (this.getDeletedAt() == null) {
            throw new IllegalStateException("이미 활성 상태인 좋아요입니다.");
        }

        // 소프트 삭제 복원
        this.restore();

        // 도메인 이벤트 발행 (좋아요 증가)
        registerEvent(new LikeChangedEvent(
                this.productId,
                this.userId,
                LikeChangedEvent.LikeAction.LIKE,
                +1
        ));
    }

    /**
     * 좋아요 삭제 시 도메인 이벤트 발행
     * <p>
     * 좋아요를 소프트 삭제하고 좋아요 감소 이벤트를 발행합니다.
     */
    public void deleteWithEvent() {
        if (this.getDeletedAt() != null) {
            throw new IllegalStateException("이미 삭제된 좋아요입니다.");
        }

        // 소프트 삭제
        this.delete();

        // 도메인 이벤트 발행 (좋아요 감소)
        registerEvent(new LikeChangedEvent(
                this.productId,
                this.userId,
                LikeChangedEvent.LikeAction.UNLIKE,
                -1
        ));
    }
}
