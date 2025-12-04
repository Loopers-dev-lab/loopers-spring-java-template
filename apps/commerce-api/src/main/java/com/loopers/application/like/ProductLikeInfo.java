package com.loopers.application.like;

import com.loopers.domain.like.ProductLike;

public record ProductLikeInfo(
        Long id,
        Long likeUserIdx,
        Long productIdx
) {
    public static ProductLikeInfo from(ProductLike productLike) {
        return new ProductLikeInfo(
                productLike.getId(),
                productLike.getLikeUser().getId(),
                productLike.getLikeProduct().getId()
        );
    }
}
