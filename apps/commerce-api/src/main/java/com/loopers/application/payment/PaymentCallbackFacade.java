package com.loopers.application.payment;

import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.interfaces.api.payment.PaymentCallbackRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackFacade {

  private final PaymentService paymentService;
  private final OrderService orderService;
  private final Clock clock;

  @Transactional
  public void handleCallback(PaymentCallbackRequest request) {
    Objects.requireNonNull(request, "request는 null일 수 없습니다.");

    if (!request.isSuccess() && !request.isFailed()) {
      log.warn("알 수 없는 결제 상태입니다. transactionKey={}, status={}", request.transactionKey(), request.status());
      return;
    }

    if (request.isSuccess()) {
      handleSuccess(request.transactionKey());
      return;
    }

    handleFailed(request.transactionKey(), request.reason());
  }

  @Transactional
  public void handleSuccess(String transactionKey) {
    Objects.requireNonNull(transactionKey, "transactionKey는 null일 수 없습니다.");
    LocalDateTime completedAt = LocalDateTime.now(clock);

    Payment payment = paymentService.getByTransactionKeyWithLock(transactionKey);

    if (payment.isCompleted()) {
      log.info("이미 처리된 결제입니다. transactionKey={}", transactionKey);
      return;
    }

    orderService.getById(payment.getOrderId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

    paymentService.toSuccess(payment, completedAt);

    log.info("결제 성공 처리 완료. transactionKey={}, orderId={}", transactionKey, payment.getOrderId());
  }

  @Transactional
  public void handleFailed(String transactionKey, String reason) {
    Objects.requireNonNull(transactionKey, "transactionKey는 null일 수 없습니다.");
    LocalDateTime completedAt = LocalDateTime.now(clock);

    Payment payment = paymentService.getByTransactionKeyWithLock(transactionKey);

    if (payment.isCompleted()) {
      log.info("이미 처리된 결제입니다. transactionKey={}", transactionKey);
      return;
    }

    String safeReason = Objects.requireNonNullElse(reason, "UNKNOWN");
    paymentService.toFailed(payment, safeReason, completedAt);

    log.info("결제 실패 처리 완료. transactionKey={}, orderId={}, reason={}",
        transactionKey, payment.getOrderId(), safeReason);
  }
}
