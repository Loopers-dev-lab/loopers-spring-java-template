package com.loopers.application.like;

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
@Transactional(readOnly = true)
public class LikeFacade {

    private final LikeService likeService;
    private final UserService userService;
    private final ProductService productService;

    @Transactional
    public void addLike(String userId, Long productId) {
        User user = userService.getUserByUserId(userId);
        Product product = productService.getProduct(productId);
        likeService.addLike(user, product);
    }

    @Transactional
    public void removeLike(String userId, Long productId) {
        User user = userService.getUserByUserId(userId);
        Product product = productService.getProduct(productId);
        likeService.removeLike(user, product);
    }
}
