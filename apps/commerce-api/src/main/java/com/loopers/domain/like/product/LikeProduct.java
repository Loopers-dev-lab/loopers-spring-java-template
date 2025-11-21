package com.loopers.domain.like.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "tb_like_product")
@Getter
public class LikeProduct extends BaseEntity {
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
