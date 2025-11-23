package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LikeDomainService {

    public void applyLike(Long userId, Product product, Like like, boolean isNew) {

        // 이미 좋아요 상태 인 경우
        if (like.isActive() && !isNew) {
            return;
        }

        like.like();
        product.increaseLikeCount();
    }

    public void applyUnLike(Long userId, Product product, Like like) {

        // 등록된 좋아요가 없거나, 이미 좋아요 취소 상태인 경우
        if (like == null || !like.isActive()) {
            return;
        }

        // 좋아요 상태 -> 좋아요 취소
        like.unlike();
        product.decreaseLikeCount();
    }
}
