package com.loopers.domain.payment.event;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderCompensationEvent;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.*;
import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PgFeignClient pgFeignClient;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentEventPublisher paymentEventPublisher;

    @Value("${pg.api.callbackUrl}")
    private String pgCallbackUrl;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("PaymentEventListener: OrderCreatedEvent 수신 - orderId: {}", event.orderId());

        Order order = orderService.findOrderById(event.orderId());
        BigDecimal finalAmount = order.getFinalAmount();
        ApiResponse<PaymentDto.PgResponse> pgApiResponse = null;
        PaymentDto.PgResponse pgResponse = null;

        try {
            if (finalAmount.compareTo(BigDecimal.ZERO) > 0) {
                pgApiResponse = pgFeignClient.approvePayment(
                        event.userId(),
                        PaymentDto.PgRequest.builder()
                                .orderId(order.getOrderIdAsString())
                                .cardNo("1111-2222-3333-4444")
                                .cardType(PaymentDto.CardType.SAMSUNG)
                                .amount(finalAmount.longValue())
                                .callbackUrl(pgCallbackUrl)
                                .build()
                );

                if (pgApiResponse != null && pgApiResponse.data() != null) {
                    pgResponse = pgApiResponse.data();
                }
            }

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
                paymentEventPublisher.publishPaymentProcess(PaymentProcessEvent.success(event.orderId()));
            } else {
                String failureReason = pgResponse != null ? pgResponse.reason() : "결제 요청에 실패했습니다.";
                log.error("PG API 호출 실패 - orderId: {}, reason: {}", event.orderId(), failureReason);
                paymentEventPublisher.publishPaymentProcess(PaymentProcessEvent.failure(event.orderId(), failureReason));
            }

        } catch (CallNotPermittedException e) {
            log.warn("PG API Circuit Breaker OPEN - 호출이 차단되었습니다: {}", e.getMessage());
            paymentEventPublisher.publishPaymentProcess(PaymentProcessEvent.failure(event.orderId(), "PG사 시스템 장애로 인해 결제가 불가능합니다."));
        } catch (Exception e) {
            log.error("PG 결제 요청 중 예외 발생: {}", e.getMessage(), e);
            paymentEventPublisher.publishPaymentProcess(PaymentProcessEvent.failure(event.orderId(), "결제 시스템 오류로 처리 중 예외가 발생했습니다."));
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCompensation(OrderCompensationEvent event) {
        log.warn("PaymentEventListener: OrderCompensationEvent 수신 - orderId: {}. PG사 결제 취소 로직 필요.", event.orderId());
        // TODO: PG사 결제 취소 API 호출 로직 구현
        // 이 로직은 결제가 이미 승인된 경우에만 호출되어야 하며,
        // PG사의 트랜잭션 상태를 확인하고 취소 요청을 보내야 합니다.
    }
}
