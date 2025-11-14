package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserLikeProductFacade {
    private final ProductLikeService productLikeService;
    private final ProductService productService;

    @Transactional
    public void userLikeProduct(Long userPkId, Long productId) {
        productService.getProductDetail(productId);
        productLikeService.like(userPkId, productId);
    }

    @Transactional
    public void userUnlikeProduct(Long userPkId, Long productId) {
        productService.getProductDetail(productId);
        productLikeService.dislike(userPkId, productId);
    }
}
