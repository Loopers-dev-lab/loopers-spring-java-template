package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.ProductLikeCountService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeService likeService;
    private final ProductService productService;
    private final ProductLikeCountService productLikeCountService;

    @Transactional
    public void addLike(Long userId, Long productId) {
        boolean isNew = likeService.createLike(Like.create(userId, productId));
        if (isNew) {
            productLikeCountService.reflectLike(productId);
        }
    }

    @Transactional
    public void removeLike(Long userId, Long productId) {
        boolean isDeleted = likeService.deleteLike(Like.create(userId, productId));

        if(isDeleted) {
            productLikeCountService.reflectUnlike(productId);
        }
    }

//    public List<Product> getLikedProducts(Long userId) {
//        List<Like> likes = likeService.findActiveLikesByUserId(userId);
//
//        List<Long> productIds = likes.stream()
//                .map(Like::getProductId)
//                .toList();
//
//        if (productIds.isEmpty()) {
//            return List.of();
//        }
//
//        return productService.findAll(productIds);
//    }
}
