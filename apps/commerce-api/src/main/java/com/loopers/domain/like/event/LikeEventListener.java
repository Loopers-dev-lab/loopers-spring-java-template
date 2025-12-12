package com.loopers.domain.like.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Like 내부 이벤트 리스너
 * 좋아요 저장/삭제 완료 후 집계 이벤트를 발행함
 * 트랜잭션 커밋 후 처리되어 집계 로직 실패가 좋아요 처리에 영향을 주지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventListener {

    private final LikeEventPublisher likeEventPublisher;

    /**
     * 상품 좋아요 저장 완료 이벤트 처리
     * 트랜잭션 커밋 후 집계 이벤트 발행
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProductLikeSaved(LikeEvents.ProductLikeSaved event) {
        try {
            log.info("상품 좋아요 저장 이벤트 수신: productId={}", event.productId());
            likeEventPublisher.publishLikeCountChanged(
                    LikeEvents.LikeCountChanged.increment(event.productId())
            );
            log.info("상품 좋아요 수 변경 이벤트 발행: productId={}, delta=+1", event.productId());
        } catch (Exception e) {
            log.error("상품 좋아요 수 변경 이벤트 발행 실패: productId={}", event.productId(), e);
            // 집계 이벤트 발행 실패는 로그만 남기고 예외를 전파하지 않음
            // 좋아요 처리는 이미 완료되었으므로 영향 없음
        }
    }

    /**
     * 상품 좋아요 삭제 완료 이벤트 처리
     * 트랜잭션 커밋 후 집계 이벤트 발행
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProductLikeDeleted(LikeEvents.ProductLikeDeleted event) {
        try {
            log.info("상품 좋아요 삭제 이벤트 수신: productId={}", event.productId());
            likeEventPublisher.publishLikeCountChanged(
                    LikeEvents.LikeCountChanged.decrement(event.productId())
            );
            log.info("상품 좋아요 수 변경 이벤트 발행: productId={}, delta=-1", event.productId());
        } catch (Exception e) {
            log.error("상품 좋아요 수 변경 이벤트 발행 실패: productId={}", event.productId(), e);
            // 집계 이벤트 발행 실패는 로그만 남기고 예외를 전파하지 않음
            // 좋아요 처리는 이미 완료되었으므로 영향 없음
        }
    }
}

