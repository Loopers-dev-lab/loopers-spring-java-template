package com.loopers.application.event;

import com.loopers.application.payment.PgClient;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
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
public class CouponEventHandler {
  private final CouponService couponService;
  private final PgClient pgClient;
  private final OrderService orderService;
  private final ApplicationEventPublisher eventPublisher;


  @TransactionalEventListener(phase = AFTER_COMMIT)
  @Async
  public void handleCouponUsed(CouponUsedEvent event) {
    try {
      Money finalPrice = couponService.useCouponById(
          event.couponIssueId(),
          event.userId(),
          event.totalPrice()
      );

      pgClient.requestPayment(event.orderId(), event.cardType(), event.cardNo(), finalPrice);


      log.info("쿠폰 사용 및 결제 처리 완료 - orderId: {}, userId: {}", event.orderId(), event.userId());

    } catch (Exception e) {
      log.error("쿠폰 사용 처리 실패 - orderId: {}, userId: {}, error: {}",
          event.orderId(), event.userId(), e.getMessage());
      orderService.cancelPayment(event.orderId());
    }
  }
}
