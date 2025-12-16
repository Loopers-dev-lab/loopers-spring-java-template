package com.loopers.application.like;

import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.like.event.ProductLikeAddedEvent;
import com.loopers.domain.like.event.ProductLikeRemovedEvent;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeEventListener {

    private final ProductService productService;
    private final ProductLikeService productLikeService;

    /**
     * 좋아요 추가 시 집계 처리
     * - 집계 이벤트는 별도 트랜잭션으로 처리( 집계 로직의 성공/실패와 상관 없이, 좋아요 처리는 정상적으로 완료되어야함 )
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductLikeAdded(ProductLikeAddedEvent event) {
        log.info("좋아요 추가 집계 처리 이벤트 시작 - ProductId: {}", event.getProductId());

        // Pessimistic Lock으로 동시성 제어
        Product product = productService.getProductWithLock(event.getProductId());

        // 좋아요 수만 증가 (ProductLike는 별도 트랜잭션에서 이미 저장됨)
        product.incrementLikeCount();

        log.info("좋아요 추가 집계 처리 이벤트 완료 - ProductId: {}, 현재 좋아요 수: {}",
                event.getProductId(), product.getLikeCount());
    }

    /**
     * 좋아요 취소 시 집계 처리
     * - 집계 이벤트는 별도 트랜잭션으로 처리( 집계 로직의 성공/실패와 상관 없이, 좋아요 처리는 정상적으로 완료되어야함 )
     * */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductLikeRemoved(ProductLikeRemovedEvent event) {
        log.info("좋아요 취소 집계 처리 시작 - ProductId: {}", event.getProductId());

        // Pessimistic Lock으로 동시성 제어
        Product product = productService.getProductWithLock(event.getProductId());

        // 좋아요 수만 감소 (ProductLike는 별도 트랜잭션에서 이미 삭제됨)
        if (product.getLikeCount() > 0) {
            product.decrementLikeCount();
        } else {
            log.warn("좋아요 수가 이미 0입니다. decrement 스킵 - ProductId: {}", event.getProductId());
        }

        log.info("좋아요 취소 집계 처리 이벤트 완료 - ProductId: {}, 현재 좋아요 수: {}",
                event.getProductId(), product.getLikeCount());
    }
}
