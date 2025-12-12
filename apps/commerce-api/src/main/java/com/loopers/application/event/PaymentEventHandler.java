package com.loopers.application.event;

import com.loopers.application.payment.TransactionStatus;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventHandler {
  private final OrderService orderService;
  private final PointService pointService;
  private final PaymentService paymentService;
  private final StockService stockService;
  private final CouponService couponService;
  private final ApplicationEventPublisher eventPublisher;

  @EventListener
  @Transactional
  public void handlePaymentCallback(PaymentCallbackEvent event) {
    log.info("결제 콜백 처리 - 주문ID: {}, 상태: {}, 사유: {}",
        event.orderId(), event.status(), event.reason());

    if (event.status() == TransactionStatus.SUCCESS) {
      handlePaymentSuccess(event);
    } else {
      handlePaymentFailure(event);
    }
  }

  private void handlePaymentSuccess(PaymentCallbackEvent event) {
    log.info("결제 성공 처리 - 주문ID: {}", event.orderId());
    Order order = orderService.getOrder(event.orderId());

    //포인트 적립
    pointService.earnFromPayment(event.orderId(), Money.wons(event.amount()));

    //결재 완료
    orderService.completePayment(event.orderId());

    Payment payment = paymentService.findPaymentByOrderId(event.orderId());

    eventPublisher.publishEvent(new PaymentDataTransferEvent(
        event.orderId(),
        order.getRefUserId(),
        BigDecimal.valueOf(event.amount()),
        payment.getCardType(),
        payment.getStatus(),
        event.status(),
        event.reason(),
        LocalDateTime.now(),
        "PAYMENT_SUCCESS"
    ));

    eventPublisher.publishEvent(new OrderDataTransferEvent(
        event.orderId(),
        order.getRefUserId(),
        order.getStatus(),
        order.getTotalPrice().getAmount(),
        LocalDateTime.now(),
        "ORDER_PAID"
    ));
  }

  private void handlePaymentFailure(PaymentCallbackEvent event) {
    log.info("결제 실패 처리 - 주문ID: {}, 사유: {}", event.orderId(), event.reason());

    Order order = orderService.getOrder(event.orderId());
    orderService.cancelPayment(event.orderId());

    Payment payment = paymentService.findPaymentByOrderId(event.orderId());
    eventPublisher.publishEvent(new PaymentDataTransferEvent(
        event.orderId(),
        order.getRefUserId(),
        BigDecimal.valueOf(event.amount()),
        payment.getCardType(),
        payment.getStatus(),
        event.status(),
        event.reason(),
        LocalDateTime.now(),
        "PAYMENT_FAILED"
    ));

  }
}
