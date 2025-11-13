package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좋아요 도메인 엔티티.
 * <p>
 * 사용자와 상품 간의 좋아요 관계를 관리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "`like`")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Like extends BaseEntity {
    @Column(name = "ref_user_id", nullable = false)
    private Long userId;

    @Column(name = "ref_product_id", nullable = false)
    private Long productId;

    /**
     * Like 인스턴스를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     */
    public Like(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    /**
     * Like 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 생성된 Like 인스턴스
     */
    public static Like of(Long userId, Long productId) {
        return new Like(userId, productId);
    }
}

