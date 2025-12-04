package com.loopers.application.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductLikeFacade {

    private final UserService userService;
    private final ProductService productService;
    private final ProductLikeService productLikeService;

    @Transactional
    public ProductLikeInfo addLike(Long productId, String userId) {
        // User 정보 조회
        User user = userService.getUser(userId);

        // Product 정보 조회 (동시성 제어를 위해 비관적 락 사용)
        Product product = productService.getProductWithLock(productId);

        ProductLike saved = productLikeService.addLike(user, product);

        return ProductLikeInfo.from(saved);
    }
}
