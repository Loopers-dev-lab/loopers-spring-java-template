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


  @EventListener
  @Transactional
  public void handlePaymentSuccess(PaymentSuccessEvent event) {
    log.info("결제 성공 처리 - 주문ID: {}, 사용자ID: {}", event.orderId(), event.userId());
    
    //포인트 적립
    pointService.earnFromPayment(event.userId(), event.amount());

    //결제 완료
    orderService.completePayment(event.orderId());
  }

  @EventListener
  @Transactional
  public void handlePaymentFailure(PaymentFailureEvent event) {
    log.info("결제 실패 처리 - 주문ID: {}, 사용자ID: {}, 사유: {}",
        event.orderId(), event.userId(), event.reason());

    orderService.cancelPayment(event.orderId());
  }
}
