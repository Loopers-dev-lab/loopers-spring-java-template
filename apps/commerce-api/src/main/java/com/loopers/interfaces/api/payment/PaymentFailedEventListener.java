package com.loopers.interfaces.api.payment;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.payment.PaymentFailedEvent;
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
public class PaymentFailedEventListener {

    private final OrderFacade orderFacade;

    /**
     * 결제 실패 이벤트 처리
     * - AFTER_COMMIT: 결제 실패 처리가 완전히 커밋된 후 실행
     * - @Async: 비동기로 실행하여 메인 트랜잭션과 분리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("=== 결제 실패 이벤트 처리 시작: orderId={}, reason={} ===",
                event.orderId(), event.reason());

        try {
            // 보상 트랜잭션: 재고 복구, 쿠폰 복구, 주문 취소
            orderFacade.cancelOrder(event.orderId(), event.couponId());

            log.info("결제 실패 이벤트 처리 완료: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("결제 실패 이벤트 처리 실패: orderId={}", event.orderId(), e);
        }
    }
}
