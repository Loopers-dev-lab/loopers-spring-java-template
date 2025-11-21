package com.loopers.application.like;

import java.time.ZonedDateTime;

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
        String productName,
        ZonedDateTime createdAt
) {
    /**
     * Creates a LikeInfo containing the username, product id and name, and the like's creation timestamp.
     *
     * @param like    the LikeEntity providing the like's creation timestamp
     * @param product the ProductEntity providing the product id and name
     * @param user    the UserEntity providing the username
     * @return a LikeInfo populated with username, productId, productName, and createdAt
     */
    public static LikeInfo of(LikeEntity like, ProductEntity product, UserEntity user) {
        return new LikeInfo(
                user.getUsername(),
                product.getId(),
                product.getName(),
                like.getCreatedAt()
        );
    }
}