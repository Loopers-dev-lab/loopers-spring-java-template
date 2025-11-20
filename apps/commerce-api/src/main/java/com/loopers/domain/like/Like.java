package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_like_user_product", columnNames = {"user_id", "product_id"})
        }
)
@Entity
public class Like extends BaseEntity {
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    public static Like create(final Long userId, final Long productId) {
        validateLike(userId, productId);
        Like like = new Like();
        like.userId = userId;
        like.productId = productId;
        return like;
    }

    private static void validateLike(final Long userId, final Long productId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저는 비어있을 수 없습니다.");
        }
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품은 비어있을 수 없습니다.");
        }
    }
}
