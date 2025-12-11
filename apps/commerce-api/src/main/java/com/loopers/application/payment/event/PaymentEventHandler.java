package com.loopers.application.payment.event;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.event.PaymentSuccessEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 이벤트를 처리하는 핸들러
 * - 결제 성공 시 주문 상태를 PAID로 변경
 * - 결제 실패 시 주문 상태를 PAYMENT_FAILED로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final OrderRepository orderRepository;

    /**
     * 결제 성공 이벤트 처리
     * - 주문 상태를 PAID로 변경
     * - 독립적인 트랜잭션으로 실행 (결제 트랜잭션과 분리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("결제 성공 이벤트 처리 - paymentId: {}, orderId: {}",
            event.paymentId(), event.orderId());

        try {
            Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

            order.markAsPaid();
            orderRepository.save(order);

            log.info("주문 상태 업데이트 완료 - orderId: {}, status: PAID", event.orderId());
        } catch (Exception e) {
            log.error("결제 성공 이벤트 처리 실패 - orderId: {}", event.orderId(), e);
            // Eventual Consistency: 실패해도 결제는 성공 상태로 유지
            // TODO: 재시도 큐에 등록하거나 관리자 알림
        }
    }

    /**
     * 결제 실패 이벤트 처리
     * - 주문 상태를 PAYMENT_FAILED로 변경
     * - 독립적인 트랜잭션으로 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 처리 - paymentId: {}, orderId: {}, reason: {}",
            event.paymentId(), event.orderId(), event.failureReason());

        try {
            Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

            order.markAsPaymentFailed();
            orderRepository.save(order);

            log.info("주문 상태 업데이트 완료 - orderId: {}, status: PAYMENT_FAILED", event.orderId());

            // TODO: 보상 트랜잭션 (재고 복구, 쿠폰 복구 등) 고려
            // TODO: 고객에게 결제 실패 알림
        } catch (Exception e) {
            log.error("결제 실패 이벤트 처리 실패 - orderId: {}", event.orderId(), e);
            // TODO: 재시도 큐에 등록하거나 관리자 알림
        }
    }
}
