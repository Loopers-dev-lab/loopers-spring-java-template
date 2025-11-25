package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class LikeFacade {
    private final LikeService likeService;
    private final ProductService productService;

    @Transactional
    public LikeInfo createLike(final Long userId, final Long productId) {
        Like like = likeService.createLike(userId, productId);
        int inserted = likeService.saveLike(userId, productId);
        if (inserted > 0) {
            productService.likeCountIncrease(productId, 1);
        }
        return LikeInfo.from(like);
    }

    @Transactional
    public void unLike(final Long userId, final Long productId) {
        likeService.unLike(userId, productId);
        productService.likeCountDecrease(productId, 1);
    }
}
