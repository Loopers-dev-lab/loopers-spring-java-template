package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentFailedEvent;
import com.loopers.domain.payment.PaymentService;
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

        // 이미 처리된 결제는 스킵
        if (!payment.isPending()) {
            log.warn("이미 처리된 결제 (멱등성): transactionId={}, currentStatus={}",
                    callback.transactionId(), payment.getStatus());
            return;
        }

        // 결제 상태 업데이트
        paymentService.updatePaymentStatus(payment, callback.status());

        Order order = orderService.getOrderById(payment.getOrderId());

        if (payment.isSuccess()) {
            // 결제 성공 → 주문 완료
            log.info("결제 성공 처리: orderId={}, paymentId={}",
                    order.getId(), payment.getId());
            order.markAsCompleted();
            orderService.save(order);

        } else {
            // 결제 실패 → 주문 실패 + 이벤트 발행 (보상 트랜잭션)
            log.warn("결제 실패 처리: orderId={}, paymentId={}, reason={}",
                    order.getId(), payment.getId(), callback.message());

            order.markAsPaymentFailed();
            orderService.save(order);

            // 결제 실패 이벤트 발행 (트랜잭션 커밋 후 실행)
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
