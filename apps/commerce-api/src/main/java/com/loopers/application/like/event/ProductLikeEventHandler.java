package com.loopers.application.like.event;

import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeEventHandler {

    private final ProductRepository productRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductLiked(ProductLikedEvent event) {
        try {
            if (event.isLiked()) {
                productRepository.incrementLikeCount(event.getProductId());
            } else {
                productRepository.decrementLikeCount(event.getProductId());
            }
            log.info("좋아요 집계 완료: productId={}, liked={}", event.getProductId(), event.isLiked());
        } catch (Exception e) {
            // 집계 실패해도 좋아요 기록은 이미 저장됨
            // 배치에서 일관성 보정 예정
            log.warn("좋아요 집계 실패: productId={}, liked={}", event.getProductId(), event.isLiked(), e);
        }
    }
}
