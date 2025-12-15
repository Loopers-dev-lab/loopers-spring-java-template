package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.stock.StockService;
import com.loopers.infrastructure.ResilientPgClient;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {
    private final ResilientPgClient resilientPgClient;

    private final PointService pointService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final StockService stockService;

    @Transactional
    public void pay(Long orderId) {
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
               PaymentV1Dto.PaymentResponse response = resilientPgClient.requestPayment(PaymentV1Dto.PgPaymentRequest.from(payment));
               paymentService.saveTransactionKey(orderId, response.transactionKey());
           }
           case POINT -> {
               try {
                   pointService.usePoint(payment.getUserId(), payment.getAmount());
               } catch (IllegalArgumentException e) {
                   throw new CoreException(ErrorType.BAD_REQUEST, e.getMessage());
               }

           }
           default -> throw new CoreException(ErrorType.BAD_REQUEST, "결제 방식이 선택되지 않았습니다.");
       }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailure(Long userId, Long orderId) {
        Order order = orderService.findOrderWithItems(userId, orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다." + orderId));

        // 재고 원복
        stockService.increaseStocks(order.getItems());
        // 실패 상태 변경
        orderService.markFailed(orderId);
    }

    public void handlePaymentSucceed(Long orderId) {
        orderService.markPaid(orderId);
    }
}
