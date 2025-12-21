package com.loopers.application.payment;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.event.PaymentSucceededEvent;
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
public class PaymentEventHandler {

  private final OrderService orderService;
  private final ProductService productService;
  private final PointService pointService;
  private final CouponService couponService;

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handlePaymentSucceeded(PaymentSucceededEvent event) {
    log.info("[Event:PaymentSucceeded] orderId={}, paymentId={}", event.orderId(), event.paymentId());

    Order order = orderService.getWithItemsById(event.orderId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

    StockDecreaseResult result = productService.tryDecreaseStocks(order.getItems(), order.getId());
    if (result.isFailure()) {
      handleStockInsufficient(order, event);
      return;
    }

    orderService.completeOrder(order.getId(), event.completedAt());
  }

  private void handleStockInsufficient(Order order, PaymentSucceededEvent event) {
    orderService.failPaymentOrder(order.getId());

    pointService.refund(order.getUserId(), order.getPointUsedAmountValue());
    couponService.restoreCoupon(order.getCouponId());

    log.warn("[Event:PaymentSucceeded] STOCK_INSUFFICIENT: orderId={}, paymentId={}", order.getId(), event.paymentId());
  }

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handlePaymentFailed(PaymentFailedEvent event) {
    log.info("[Event:PaymentFailed] orderId={}, paymentId={}, reason={}", event.orderId(), event.paymentId(), event.reason());

    Order order = orderService.getById(event.orderId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

    orderService.failPaymentOrder(order.getId());

    if (order.getPointUsedAmountValue() > 0) {
      pointService.refund(order.getUserId(), order.getPointUsedAmountValue());
    }

    if (order.getCouponId() != null) {
      couponService.restoreCoupon(order.getCouponId());
    }
  }
}
