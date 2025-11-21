package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeDomainService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeTxService {
    private final LikeService likeService;
    private final ProductService productService;
    private final LikeDomainService likeDomainService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addTxLike(Long userId, Long productId) {
        Product product = productService.getProduct(productId);
        Optional<Like> like = likeService.findLike(userId, productId);

        if (like.isEmpty()) {
            likeDomainService.applyLike(userId, product, likeService.createLike(Like.create(userId, productId)), true);
        } else {
            likeDomainService.applyLike(userId, product, like.get(), false);
        }
    }

    @Transactional
    public void removeTxLike(Long userId, Long productId) {
        Product product = productService.getProduct(productId);
        Optional<Like> like = likeService.findLike(userId, productId);

        likeDomainService.applyUnLike(userId, product, like.orElse(null));
    }
}
