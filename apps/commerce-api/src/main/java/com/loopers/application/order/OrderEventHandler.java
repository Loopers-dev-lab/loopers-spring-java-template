package com.loopers.application.order;

import static org.springframework.transaction.event.TransactionPhase.*;

import com.loopers.application.common.DataPlatformClient;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.StockDecreaseResult;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

  private final OrderService orderService;
  private final ProductService productService;
  private final CouponService couponService;
  private final PointService pointService;
  private final DataPlatformClient dataPlatformClient;

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handlePointDeduction(OrderCreatedEvent event) {
    if (!event.hasPointUsage()) {
      log.debug("[Event:OrderCreated:Point] NO_POINT_USAGE orderId={}", event.orderId());
      return;
    }

    log.info("[Event:OrderCreated:Point] orderId={}, amount={}", event.orderId(), event.pointAmount());
    pointService.deduct(event.userId(), Money.of(event.pointAmount()));
  }

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handleCouponUsage(OrderCreatedEvent event) {
    if (!event.hasCoupon()) {
      log.debug("[Event:OrderCreated:Coupon] NO_COUPON orderId={}", event.orderId());
      return;
    }

    log.info("[Event:OrderCreated:Coupon] orderId={}, couponId={}", event.orderId(), event.couponId());
    couponService.useCoupon(event.couponId(), event.userId(), event.orderId());
  }

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handlePointOnlyPayment(OrderCreatedEvent event) {
    if (requiresPgPayment(event)) {
      log.debug("[Event:OrderCreated:PointOnlyPayment] PG_PAYMENT_REQUIRED orderId={}", event.orderId());
      return;
    }

    log.info("[Event:OrderCreated:PointOnlyPayment] orderId={}, userId={}", event.orderId(), event.userId());

    Order order = orderService.getWithItemsById(event.orderId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

    StockDecreaseResult result = productService.tryDecreaseStocks(order.getItems(), order.getId());
    if (result.isFailure()) {
      handleStockInsufficient(order);
      return;
    }

    orderService.completeOrder(order.getId(), event.orderedAt());
  }

  private boolean requiresPgPayment(OrderCreatedEvent event) {
    return event.pgAmount() != null && event.pgAmount() > 0;
  }

  private void handleStockInsufficient(Order order) {
    orderService.failPaymentOrder(order.getId());

    pointService.refund(order.getUserId(), order.getPointUsedAmountValue());
    couponService.restoreCoupon(order.getCouponId());

    log.warn("[Event:OrderCreated:PointOnlyPayment] STOCK_INSUFFICIENT: orderId={}", order.getId());
  }

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handleOrderCompleted(OrderCompletedEvent event) {
    log.info("[Event:OrderCompleted] orderId={}, userId={}", event.orderId(), event.userId());
    dataPlatformClient.send(event);
  }
}
