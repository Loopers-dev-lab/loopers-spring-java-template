package com.loopers.domain.like;

import static java.util.Objects.requireNonNull;

import com.loopers.domain.BaseEntity;

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
public class LikeEntity extends BaseEntity {

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
}
