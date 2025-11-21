package com.loopers.application.like;

import com.loopers.domain.like.LikeEntity;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.user.UserEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 12.
 */
public record LikeInfo(
        String username,
        Long productId,
        String productName
) {
    public static LikeInfo of(LikeEntity like, ProductEntity product, UserEntity user) {
        return new LikeInfo(
                user.getUsername(),
                product.getId(),
                product.getName()
        );
    }
}
