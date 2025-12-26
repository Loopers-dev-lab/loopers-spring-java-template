package com.loopers.infrastructure.outbox;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.application.event.order.OrderPlacedEvent;
import com.loopers.application.event.payment.PaymentCompletedEvent;
import com.loopers.application.event.product.ProductViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kafka Outbox Event Listener
 * - Application Event를 받아서 Outbox 테이블에 저장
 * - BEFORE_COMMIT: 같은 트랜잭션 내에서 처리되어 원자성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxEventListener {

    private final OutboxEventWriter outboxWriter;

    /**
     * 주문 생성 이벤트 → Outbox 저장
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.debug("[Outbox] OrderPlacedEvent 수신 - orderNo: {}", event.orderNo());

        outboxWriter.write(
            event.orderNo(),           // partition key
            "ORDER_PLACED",            // event type
            event                      // payload
        );
    }

    /**
     * 주문 완료 이벤트 → Outbox 저장
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.debug("[Outbox] OrderCompletedEvent 수신 - orderNo: {}", event.orderNo());

        outboxWriter.write(
            event.orderNo(),
            "ORDER_COMPLETED",
            event
        );
    }

    /**
     * 결제 완료 이벤트 → Outbox 저장
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.debug("[Outbox] PaymentCompletedEvent 수신 - orderNo: {}", event.orderNo());

        outboxWriter.write(
            event.orderNo(),
            "PAYMENT_COMPLETED",
            event
        );
    }

    /**
     * 상품 좋아요 이벤트 → Outbox 저장
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProductLiked(ProductLikedEvent event) {
        log.debug("[Outbox] ProductLikedEvent 수신 - productId: {}", event.productId());

        outboxWriter.write(
            String.valueOf(event.productId()),  // partition key
            "PRODUCT_LIKED",
            event
        );
    }

    /**
     * 상품 좋아요 취소 이벤트 → Outbox 저장
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProductUnliked(ProductUnlikedEvent event) {
        log.debug("[Outbox] ProductUnlikedEvent 수신 - productId: {}", event.productId());

        outboxWriter.write(
            String.valueOf(event.productId()),
            "PRODUCT_UNLIKED",
            event
        );
    }

    /**
     * 상품 조회 이벤트 → Outbox 저장
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductViewed(ProductViewedEvent event) {
        log.debug("[Outbox] ProductViewedEvent 수신 - productId: {}", event.productId());

        outboxWriter.write(
            String.valueOf(event.productId()),  // partition key
            "PRODUCT_VIEWED",
            event
        );
    }
}
