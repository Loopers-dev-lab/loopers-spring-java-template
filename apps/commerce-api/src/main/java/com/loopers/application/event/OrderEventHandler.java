package com.loopers.application.event;

import com.loopers.application.payment.PgClient;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
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
  private final PgClient pgClient;
  private final ApplicationEventPublisher eventPublisher;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Async
  public void handleOrderCreated(OrderCreatedEvent event) {
    Order order = orderService.getOrder(event.orderId());

    Money finalPrice = couponService.useCouponById(
        event.couponIssueId(),
        event.userId(),
        order.getTotalPrice()
    );

    pgClient.requestPayment(event.orderId(), event.cardType(), event.cardNo(), finalPrice);

    // 데이터 플랫폼으로 주문 생성 이벤트 전송
    eventPublisher.publishEvent(new OrderDataTransferEvent(
        event.orderId(),
        event.userId(),
        order.getStatus(),
        order.getTotalPrice().getAmount(),
        LocalDateTime.now(),
        "ORDER_CREATED"
    ));
  }
}
