package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgRequestFailedException;
import com.loopers.interfaces.api.order.OrderDto.PaymentInfo;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

  private final PaymentService paymentService;
  private final PgClient pgClient;
  private final Clock clock;

  @Value("${pg-client.callback-url}")
  private String callbackUrl;

  @Transactional
  public void processPayment(Order order, PaymentInfo paymentInfo) {
    Objects.requireNonNull(order, "order는 null일 수 없습니다.");

    if (!order.hasPgAmount()) {
      return;
    }

    requestPgPayment(order, paymentInfo);
  }

  private void requestPgPayment(Order order, PaymentInfo paymentInfo) {
    if (paymentInfo == null) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_INFO_EMPTY);
    }

    Long pgAmount = order.getPgAmountValue();
    LocalDateTime requestedAt = LocalDateTime.now(clock);
    Payment payment = paymentService.create(
        order.getId(), order.getUserId(), paymentInfo.cardType(), paymentInfo.cardNo(), pgAmount, requestedAt
    );

    try {
      PgPaymentResponse pgResponse = pgClient.requestPayment(
          String.valueOf(order.getUserId()),
          PgPaymentRequest.of(order.getId(), paymentInfo.cardType(), paymentInfo.cardNo(), pgAmount, callbackUrl)
      );

      if (pgResponse.isPending()) {
        paymentService.toPending(payment.getId(), pgResponse.transactionKey());
        log.info("PG 결제 요청 완료. orderId={}, transactionKey={}", order.getId(), pgResponse.transactionKey());
      }
    } catch (PgRequestFailedException e) {
      paymentService.toRequestFailed(payment.getId());
      log.error("PG 결제 요청 실패. orderId={}, paymentId={}", order.getId(), payment.getId(), e);
      throw e;
    }
  }
}