package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponProcessedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PgPaymentGateway pgPaymentGateway;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentEventPublisher paymentEventPublisher;

    @Value("${pg.api.callbackUrl}")
    private String pgCallbackUrl;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessed(CouponProcessedEvent event) {
        log.info("PaymentEventListener: CouponProcessedEvent 수신 - orderId: {}", event.orderId());

        Order order = orderService.findOrderById(event.orderId());
        BigDecimal finalAmount = order.getFinalAmount();
        
        ApiResponse<PaymentDto.PgResponse> pgApiResponse = null;

        if (finalAmount.compareTo(BigDecimal.ZERO) > 0) {
            pgApiResponse = pgPaymentGateway.approvePayment(
                    order.getUserId(),
                    PaymentDto.PgRequest.builder()
                            .orderId(order.getOrderIdAsString())
                            .cardNo("1111-2222-3333-4444")
                            .cardType(PaymentDto.CardType.SAMSUNG)
                            .amount(finalAmount.longValue())
                            .callbackUrl(pgCallbackUrl)
                            .build()
            );
        }

        PaymentDto.PgResponse pgResponse = (pgApiResponse != null) ? pgApiResponse.data() : null;

        if (pgResponse != null && pgResponse.status() == PaymentDto.PaymentStatus.PENDING) {
            paymentService.saveCommercePayment(CommercePayment.builder()
                    .orderId(order.getId())
                    .transactionKey(pgResponse.transactionKey())
                    .method(PaymentDto.PaymentMethod.CARD)
                    .cardType(PaymentDto.CardType.SAMSUNG)
                    .cardNo("1111-2222-3333-4444")
                    .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                    .amount(finalAmount)
                    .build()
            );
            log.info("PG API 호출 및 결제 정보 저장 성공 - orderId: {}", event.orderId());
            paymentEventPublisher.publishPaymentProcessed(new PaymentProcessedEvent(event.orderId(), event));
        } else {
            String failureReason = (pgResponse != null && pgResponse.reason() != null) ? pgResponse.reason() : "결제 요청에 실패했습니다.";
            log.error("PG API 호출 실패 - orderId: {}, reason: {}", event.orderId(), failureReason);
            paymentEventPublisher.publishPaymentProcessingFailed(new PaymentProcessingFailedEvent(event.orderId(), failureReason));
        }
        
    }
}
