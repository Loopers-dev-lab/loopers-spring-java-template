package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackService {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processCallback(PaymentCallbackDto callback) {
        log.info("결제 콜백 처리 시작: transactionId={}, status={}",
                callback.transactionId(), callback.status());

        Payment payment = paymentService.getPaymentByTransactionIdWithLock(callback.transactionId());

        // 이미 처리된 결제는 스킵 (멱등성)
        if (!payment.isPending()) {
            log.warn("이미 처리된 결제 (멱등성): transactionId={}, currentStatus={}",
                    callback.transactionId(), payment.getStatus());
            return;
        }

        // 결제 상태 업데이트
        paymentService.updatePaymentStatus(payment, callback.status());

        Order order = orderService.getOrderById(payment.getOrderId());

        if (payment.isSuccess()) {
            // 결제 성공 → 이벤트로 후속 처리 위임
            log.info("결제 성공, 이벤트 발행: orderId={}, paymentId={}",
                    order.getId(), payment.getId());

            eventPublisher.publishEvent(PaymentSucceededEvent.of(payment, order.getCouponId()));

        } else {
            // 결제 실패 → 주문 실패 + 보상 트랜잭션 이벤트
            log.warn("결제 실패 처리: orderId={}, paymentId={}, reason={}",
                    order.getId(), payment.getId(), callback.message());

            order.markAsPaymentFailed();
            orderService.save(order);

            eventPublisher.publishEvent(new PaymentFailedEvent(
                    order.getId(),
                    order.getUser().getId(),
                    order.getCouponId(),
                    callback.message()
            ));
        }

        log.info("결제 콜백 처리 완료: transactionId={}, finalStatus={}",
                callback.transactionId(), payment.getStatus());
    }
}
