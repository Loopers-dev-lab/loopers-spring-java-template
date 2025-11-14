package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class UserLikeProductFacade {
    private final UserService userService;
    private final ProductLikeService productLikeService;
    private final ProductService productService;

    @Transactional
    public void userLikeProduct(Long userPkId, Long productId) {
        userService.getUser(userPkId);
        ProductModel product = productService.getProductDetail(productId);
        productService.validateProductDeleteOrStopSelling(product);
        productLikeService.like(userPkId, productId);
    }

    @Transactional
    public void userUnlikeProduct(Long userPkId, Long productId) {
        userService.getUser(userPkId);
        ProductModel product = productService.getProductDetail(productId);
        productService.validateProductDeleteOrStopSelling(product);
        productLikeService.dislike(userPkId, productId);
    }
}
