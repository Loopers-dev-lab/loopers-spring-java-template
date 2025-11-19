package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(
        name = "product_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_like_user_product",
                columnNames = {"user_id", "product_id"}  // ⚠️ 실제 컬럼명으로!
        )
)
public class ProductLike extends BaseEntity {

    private Long userId;

    private Long productId;

    protected ProductLike() {
    }

    private ProductLike(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public static ProductLike create(Long userId, Long productId) {
        validateUserId(userId);
        validateProductId(productId);

        return new ProductLike(userId, productId);
    }

    private static void validateUserId(Long userId) {
        if (Objects.isNull(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    private static void validateProductId(Long productId) {
        if (Objects.isNull(productId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }
}
