package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void like(String userId, Long productId) {
        // 상품 존재 여부 확인
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        // 멱등성 보장: 이미 좋아요한 경우 무시
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        Like like = Like.builder()
            .userId(userId)
            .productId(productId)
            .build();

        likeRepository.save(like);
    }

    @Transactional
    public void unlike(String userId, Long productId) {
        // 멱등성 보장: 좋아요하지 않은 경우에도 정상 처리
        if (!likeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }

        likeRepository.deleteByUserIdAndProductId(userId, productId);
    }

    public Page<Like> getLikesByUser(String userId, Pageable pageable) {
        return likeRepository.findByUserId(userId, pageable);
    }

    public Long getLikeCount(Long productId) {
        return likeRepository.countByProductId(productId);
    }

    public boolean isLiked(String userId, Long productId) {
        return likeRepository.existsByUserIdAndProductId(userId, productId);
    }
}
