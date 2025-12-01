package com.loopers.domain.like;

import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * packageName : com.loopers.application.like
 * fileName     : LikeService
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */
@Component
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void like(String userId, Long productId) {
        if (likeRepository.findByUserIdAndProductId(userId, productId).isPresent()) return;

        Like like = Like.create(userId, productId);
        likeRepository.save(like);
        productRepository.incrementLikeCount(productId);
    }

    @Transactional
    public void unlike(String userId, Long productId) {
        likeRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(like -> {
                    likeRepository.delete(like);
                    productRepository.decrementLikeCount(productId);
                });
    }

    @Transactional(readOnly = true)
    public long countByProductId(Long productId) {
        return likeRepository.countByProductId(productId);
    }

}
