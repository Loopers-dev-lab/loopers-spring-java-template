package com.loopers.application.event;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataTransferEventHandler {
  private final OrderService orderService;
  private final PaymentService paymentService;
  private final ApplicationEventPublisher eventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentSuccess(PaymentSuccessEvent event) {
    log.debug("Handling PaymentSuccessEvent for data transfer: orderId={}", event.orderId());

    Payment payment = paymentService.findPaymentByOrderId(event.orderId());

    // 결제 성공 데이터 전송
    eventPublisher.publishEvent(new PaymentDataTransferEvent(
        event.orderId(),
        event.userId(),
        event.amount(),
        payment.getCardType(),
        payment.getStatus(),
        com.loopers.application.payment.TransactionStatus.SUCCESS,
        event.reason(),
        LocalDateTime.now(),
        "PAYMENT_SUCCESS"
    ));

    // 주문 결제 완료 데이터 전송 (이벤트 데이터 활용)
    eventPublisher.publishEvent(new OrderDataTransferEvent(
        event.orderId(),
        event.userId(),
        null, // order.getStatus() 대신 OrderCompletedEvent에서 처리
        event.totalOrderAmount(),
        LocalDateTime.now(),
        "ORDER_PAID"
    ));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentFailure(PaymentFailureEvent event) {
    log.debug("Handling PaymentFailureEvent for data transfer: orderId={}", event.orderId());

    Payment payment = paymentService.findPaymentByOrderId(event.orderId());

    // 결제 실패 데이터 전송 (이벤트 데이터 활용)
    eventPublisher.publishEvent(new PaymentDataTransferEvent(
        event.orderId(),
        event.userId(),
        event.amount(),
        payment.getCardType(),
        payment.getStatus(),
        com.loopers.application.payment.TransactionStatus.FAILED,
        event.reason(),
        LocalDateTime.now(),
        "PAYMENT_FAILED"
    ));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCreated(OrderCreatedEvent event) {
    log.debug("Handling OrderCreatedEvent for data transfer: orderId={}", event.orderId());

    Order order = orderService.getOrder(event.orderId());

    // 주문 생성 데이터 전송
    eventPublisher.publishEvent(new OrderDataTransferEvent(
        event.orderId(),
        event.userId(),
        order.getStatus(),
        order.getTotalPrice(),
        LocalDateTime.now(),
        "ORDER_CREATED"
    ));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCancelled(OrderCancelledEvent event) {
    log.debug("Handling OrderCancelledEvent for data transfer: orderId={}", event.orderId());

    Order order = orderService.getOrder(event.orderId());

    // 주문 취소 데이터 전송
    eventPublisher.publishEvent(new OrderDataTransferEvent(
        event.orderId(),
        event.userId(),
        order.getStatus(),
        order.getTotalPrice(),
        LocalDateTime.now(),
        "ORDER_CANCELLED"
    ));
  }
}
