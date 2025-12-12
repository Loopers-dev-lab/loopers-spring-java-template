package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStrategy;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointPaymentStrategy implements PaymentStrategy {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PointService pointService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PaymentInfo pay(Payment payment) {
        log.info("포인트 결제 실행: paymentId={}, amount={}",
                payment.getId(), payment.getAmountValue());

        Payment savedPayment;

        try {
            // 포인트 차감 (락 사용)
            pointService.usePointWithLock(payment.getUserId(), payment.getAmountValue());

            // 결제 성공 처리
            payment.markAsSuccess();
            savedPayment = paymentService.save(payment);

            log.info("포인트 결제 성공: paymentId={}", payment.getId());

        } catch (CoreException e) {
            log.error("포인트 결제 실패: paymentId={}, reason={}",
                    payment.getId(), e.getMessage());

            // 결제 실패 처리
            payment.markAsFailed(e.getMessage());
            savedPayment = paymentService.save(payment);

            // 결제 실패 이벤트 발행
            Order order = orderService.getOrderById(payment.getOrderId());

            eventPublisher.publishEvent(new PaymentFailedEvent(
                    payment.getOrderId(),
                    payment.getUserId(),
                    order.getCouponId(),
                    e.getMessage()
            ));


            throw e;
        }

        return PaymentInfo.from(savedPayment);
    }
}
