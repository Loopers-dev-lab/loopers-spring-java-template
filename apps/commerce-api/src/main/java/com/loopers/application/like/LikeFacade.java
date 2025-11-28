package com.loopers.application.like;

import com.loopers.application.product.ProductCacheService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeService likeService;
    private final UserService userService;
    private final ProductService productService;
    private final ProductCacheService productCacheService;

    @Transactional
    public void addLike(String userId, Long productId) {
        User user = userService.getUserByUserId(userId);
        Product product = productService.getProduct(productId);

        boolean isNewLike = likeService.addLike(user, product);

        if (isNewLike) {
            productService.incrementLikeCount(productId);
            productCacheService.evictProductDetailCache(productId);
            productCacheService.evictProductListCachesByLikesSort();
        }
    }

    @Transactional
    public void removeLike(String userId, Long productId) {
        User user = userService.getUserByUserId(userId);
        Product product = productService.getProduct(productId);

        boolean wasRemoved = likeService.removeLike(user, product);

        if (wasRemoved) {
            productService.decrementLikeCount(productId);
            productCacheService.evictProductDetailCache(productId);
            productCacheService.evictProductListCachesByLikesSort();
        }
    }
}
