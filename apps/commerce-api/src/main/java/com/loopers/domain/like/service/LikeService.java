package com.loopers.domain.like.service;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.repository.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    public void like(String memberId, Long productId) {
        if (likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            return;
        }

        likeRepository.save(new Like(memberId, productId));

        int updated = productRepository.incrementLikeCount(productId);
        if (updated == 0) {
            throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
    }

    public void unlike(String memberId, Long productId) {
        if (!likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            return;
        }

        likeRepository.deleteByMemberIdAndProductId(memberId, productId);

        int updated = productRepository.decrementLikeCount(productId);
        if (updated == 0) {
            throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
    }
}