package com.loopers.domain.like.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
    name = "tb_like_product",
    indexes = {
        @Index(name = "idx_like_product_product_id", columnList = "product_id,deleted_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_like_product_user_product",
            columnNames = {"user_id", "product_id"}
        )
    }
)
@Getter
public class LikeProduct extends BaseEntity {
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    protected LikeProduct() {
    }

    private LikeProduct(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public static LikeProduct create(Long userId, Long productId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 1 이상이어야 합니다.");
        }
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 1 이상이어야 합니다.");
        }
        return new LikeProduct(userId, productId);
    }
}
