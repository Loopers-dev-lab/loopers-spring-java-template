package com.loopers.application.useraction;

import com.loopers.domain.like.event.LikeCreatedEvent;
import com.loopers.domain.like.event.LikeDeletedEvent;
import com.loopers.domain.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사용자 행동 추적 이벤트 핸들러
 * - 도메인 이벤트를 수신하여 사용자 행동을 자동으로 로깅
 * - 비즈니스 로직에 추적 코드를 넣지 않고 이벤트 기반으로 분리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionEventHandler {

    private final UserActionService userActionService;

    /**
     * 좋아요 생성 이벤트 처리
     * - 상품 좋아요 행동 로깅
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeCreated(LikeCreatedEvent event) {
        log.debug("좋아요 생성 이벤트 수신 - userId: {}, productId: {}",
            event.userId(), event.productId());

        userActionService.logProductLike(event.userId(), event.productId());
    }

    /**
     * 좋아요 삭제 이벤트 처리
     * - 상품 좋아요 취소 행동 로깅
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeDeleted(LikeDeletedEvent event) {
        log.debug("좋아요 삭제 이벤트 수신 - userId: {}, productId: {}",
            event.userId(), event.productId());

        userActionService.logProductUnlike(event.userId(), event.productId());
    }

    /**
     * 주문 생성 이벤트 처리
     * - 주문 생성 행동 로깅
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.debug("주문 생성 이벤트 수신 - userId: {}, orderId: {}",
            event.userId(), event.orderId());

        // 주문 상품 정보를 metadata로 저장
        String metadata = String.format("totalAmount=%s, finalAmount=%s, items=%d",
            event.totalAmount(), event.finalAmount(), event.items().size());

        userActionService.logOrderCreate(event.userId(), event.orderId(), metadata);
    }
}
