package com.loopers.interfaces.listener;

import com.loopers.application.like.LikeEvent;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class LikeCountEventListener {

    private final ProductService productService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LikeEvent.LikeRecorded event) {
        log.info("LikeRecorded 이벤트 수신 - 좋아요 수 증가: productId={}", event.productId());
        productService.increaseLikeCount(event.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LikeEvent.LikeCancelled event) {
        log.info("LikeCancelled 이벤트 수신 - 좋아요 수 감소: productId={}", event.productId());
        productService.decreaseLikeCount(event.productId());
    }
}
