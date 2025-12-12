package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentFailedEvent;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStrategy;
import com.loopers.infrastructure.pg.PgService;
import com.loopers.support.error.CoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentStrategy {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PgService pgService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${payment.callback.base-url}")
    private String callbackBaseUrl;

    @Value("${payment.callback.path}")
    private String callbackPath;

    @Override
    @Transactional
    public PaymentInfo pay(Payment payment) {
        log.info("PG 카드 결제 실행: paymentId={}, amount={}",
                payment.getId(), payment.getAmountValue());

        Payment savedPayment;

        try {
            String callbackUrl = callbackBaseUrl + callbackPath;

            String transactionId = pgService.requestPayment(
                    payment.getUserId().toString(),
                    payment.getOrderId().toString(),
                    payment.getCardType(),
                    payment.getCardNo(),
                    payment.getAmountValue().toString(),
                    callbackUrl
            );

            // 거래 ID 업데이트
            payment.updateTransactionId(transactionId);
            savedPayment = paymentService.save(payment);

            log.info("PG 결제 요청 완료 (콜백 대기): paymentId={}, transactionId={}",
                    payment.getId(), transactionId);

        } catch (Exception e) {
            log.error("PG 결제 요청 실패: paymentId={}, reason={}",
                    payment.getId(), e.getMessage());

            payment.markAsTimeout();
            savedPayment = paymentService.save(payment);

            Order order = orderService.getOrderById(payment.getOrderId());

            eventPublisher.publishEvent(new PaymentFailedEvent(
                    payment.getOrderId(),
                    payment.getUserId(),
                    order.getCouponId(),
                    e.getMessage()
            ));

            throw new CoreException(
                    com.loopers.support.error.ErrorType.INTERNAL_ERROR,
                    "PG 결제 요청 실패: " + e.getMessage()
            );
        }

        return PaymentInfo.from(savedPayment);
    }
}
