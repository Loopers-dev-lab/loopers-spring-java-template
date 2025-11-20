package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserLikeProductFacade {
    private final UserService userService;
    private final ProductLikeService productLikeService;
    private final ProductService productService;

    @Transactional
    public void userLikeProduct(LikeCommand.Like input) {
        UserModel found = userService.getUser(input.userId());
        ProductModel product = productService.getProductDetail(input.productId());
        productService.validateProductDeleteOrStopSelling(product);
        productLikeService.like(found.getId(), input.productId());
    }

    @Transactional
    public void userUnlikeProduct(LikeCommand.Like input) {
        UserModel found = userService.getUser(input.userId());
        ProductModel product = productService.getProductDetail(input.productId());
        productService.validateProductDeleteOrStopSelling(product);
        productLikeService.dislike(found.getId(), product.getId());
    }
}
