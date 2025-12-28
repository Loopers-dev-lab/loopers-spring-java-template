package com.loopers.application.event;

import com.loopers.application.payment.PgClient;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.stock.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderEventHandler {
  private final OrderService orderService;
  private final CouponService couponService;
  private final StockService stockService;
  private final PgClient pgClient;
  private final ApplicationEventPublisher eventPublisher;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Async
  public void handleOrderCreated(OrderCreatedEvent event) {
    log.debug("주문 처리 시작 - orderId: {}, couponIssueId: {}",
        event.orderId(), event.couponIssueId());

    try {
      Order order = orderService.getOrder(event.orderId());
      Money finalPrice = order.getTotalPrice();

      // 쿠폰 처리 (있는 경우만)
      if (event.couponIssueId() != null) {
        log.debug("쿠폰 사용 처리 - couponIssueId: {}", event.couponIssueId());
        finalPrice = couponService.useCouponById(
            event.couponIssueId(),
            event.userId(),
            order.getTotalPrice()
        );
      }

      // 결제 요청
      pgClient.requestPayment(order.getOrderId(), event.cardType(), event.cardNo(), finalPrice);


      log.info("주문 처리 완료 - orderId: {}, userId: {}, finalPrice: {}",
          event.orderId(), event.userId(), finalPrice.getAmount());

    } catch (Exception e) {
      log.error("주문 처리 실패 - orderId: {}, userId: {}, error: {}",
          event.orderId(), event.userId(), e.getMessage());
      orderService.cancelPayment(event.orderId());
    }
  }

  @TransactionalEventListener
  public void handleOrderCancelled(OrderCancelledEvent event) {
    Order order = orderService.getOrder(event.orderId());

    // 재고 롤백
    order.getOrderItems().forEach(orderItem ->
        stockService.restore(orderItem.getRefProductId(), orderItem.getQuantity())
    );

    // 쿠폰 사용 롤백
    if (order.getRefCouponIssueId() != null) {
      couponService.rollbackCouponUsage(order.getRefCouponIssueId());
    }

  }
}
