package com.loopers.application.payment;

import com.loopers.application.order.event.OrderCreated;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.PointService;
import com.loopers.infrastructure.ResilientPgClient;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import com.loopers.messaging.event.OrderPaidEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PointService pointService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    private final ApplicationEventPublisher eventPublisher;
    private final ResilientPgClient resilientPgClient;

    @Transactional
    public void pay(Long userId, Long orderId) {
        // 1. 결제 전 금액 검증
        Order order = orderService.findOrderById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다."));

        Payment payment = paymentService.getPaymentByOrderId(orderId);

        if (!payment.getAmount().equals(order.calculateTotalPrice())) {
            log.error("결제 금액과 주문 금액이 불일치 합니다. 실제 주문 금액 = {}, 결제 요청 금액 {}", order.calculateTotalPrice(), payment.getAmount());
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // 2. 결제 수단 별 처리
        switch (payment.getPaymentMethod()) {
           case CARD -> {
               // 카드 결제정보 검증
               eventPublisher.publishEvent(OrderCreated.of(userId, orderId));
           }
           case POINT -> {
               try {
                   pointService.usePoint(payment.getUserId(), payment.getAmount());

                   List<OrderPaidEvent.OrderItemData> items =
                           order.getItems().stream()
                                           .map(item -> new OrderPaidEvent.OrderItemData(
                                                   item.getProductId(),
                                                   item.getQuantity(),
                                                   item.getTotalPrice().getAmount()
                                           )).toList();

                   eventPublisher.publishEvent(OrderPaidEvent.of(orderId, items));
               } catch (IllegalArgumentException e) {
                   throw new CoreException(ErrorType.BAD_REQUEST, e.getMessage());
               }

           }
           default -> throw new CoreException(ErrorType.BAD_REQUEST, "결제 방식이 선택되지 않았습니다.");
       }
    }

    @Transactional
    public void payPg(Long orderId) {
        // 주문 정보 조회
        Payment payment = paymentService.getPaymentByOrderId(orderId);

        // PG 결제 진행
        PaymentV1Dto.PaymentResponse response = resilientPgClient.requestPayment(PaymentV1Dto.PgPaymentRequest.from(payment));

        // 결제 트랜잭션 키 저장
        paymentService.saveTransactionKey(orderId, response.transactionKey());
    }
}
