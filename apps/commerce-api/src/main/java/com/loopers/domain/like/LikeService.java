package com.loopers.domain.like;

import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class LikeService {
    private final LikeRepository likeRepository;
    private final ProductService productService;

    @Transactional
    public Like createLike(final Long userId, final Long productId) {
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 좋아요를 하셨습니다.");
        }
        Like like = Like.create(userId, productId);
        int inserted = likeRepository.saveIgnore(like.getUserId(), like.getProductId());
        if (inserted > 0) {
            productService.likeCountIncrease(productId, 1);
        }
        return like;
    }

    @Transactional
    public void unLike(final Long userId, final Long productId) {
        if (!likeRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 좋아요를 취소 하셨습니다.");
        }
        Like like = getLike(userId, productId);
        productService.likeCountDecrease(productId, 1);
        likeRepository.delete(like);
    }

    public Like getLike(final Long userId, final Long productId) {
        return likeRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[productId = " + productId + "] 상품의 좋아요를 찾을 수 없습니다."));
    }
}
