package com.loopers.application.event;

import com.loopers.application.payment.PgClient;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

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
    Order order = orderService.getOrder(event.orderId());

    if (event.couponIssueId() != null) {
      handleOrderWithCoupon(event, order);
    } else {
      handleOrderWithoutCoupon(event, order);
    }
  }

  private void handleOrderWithCoupon(OrderCreatedEvent event, Order order) {
    // 쿠폰 사용 이벤트 발행 (별도 트랜잭션에서 처리)
    eventPublisher.publishEvent(new CouponUsedEvent(
        event.orderId(),
        event.userId(),
        event.couponIssueId(),
        order.getTotalPrice(),
        event.cardType(),
        event.cardNo()
    ));
  }

  private void handleOrderWithoutCoupon(OrderCreatedEvent event, Order order) {
    try {
      // 쿠폰 없이 바로 결제 처리
      pgClient.requestPayment(event.orderId(), event.cardType(), event.cardNo(), order.getTotalPrice());

      // 데이터 플랫폼으로 주문 생성 이벤트 전송
      eventPublisher.publishEvent(new OrderDataTransferEvent(
          event.orderId(),
          event.userId(),
          order.getStatus(),
          order.getTotalPrice().getAmount(),
          LocalDateTime.now(),
          "ORDER_CREATED"
      ));
    } catch (Exception e) {
      // 결제 요청 실패시 주문 취소 처리
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

    // 데이터 플랫폼으로 주문 취소 이벤트 전송
    eventPublisher.publishEvent(new OrderDataTransferEvent(
        event.orderId(),
        event.userId(),
        order.getStatus(),
        order.getTotalPrice().getAmount(),
        LocalDateTime.now(),
        "ORDER_CANCELLED"
    ));
  }
}
