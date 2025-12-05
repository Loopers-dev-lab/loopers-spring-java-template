package com.loopers.infrastructure.payment;

import com.loopers.application.payment.PaymentCallbackFacade;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgTransactionResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PENDING 상태 결제 복구 스케줄러
 *
 * <p>TODO: 재시도 제한 로직 추가 필요
 * <ul>
 *   <li>retryCount 필드 추가: N회 초과 시 ABANDONED 상태로 전환</li>
 *   <li>또는 시간 기반 제한: 생성 후 10분 경과 시 복구 포기</li>
 *   <li>분산 환경: ShedLock 또는 SKIP LOCKED로 중복 처리 방지</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

  private final PaymentService paymentService;
  private final PaymentCallbackFacade paymentCallbackFacade;
  private final PgClient pgClient;
  private final Clock clock;

  @Scheduled(fixedDelay = 60000)
  public void recoverPendingPayments() {
    LocalDateTime before = LocalDateTime.now(clock).minusMinutes(1);
    List<Payment> pendingPayments = paymentService.findPendingPaymentsBefore(before);

    if (pendingPayments.isEmpty()) {
      return;
    }

    log.info("복구 대상 PENDING 결제 {}건 발견", pendingPayments.size());

    for (Payment payment : pendingPayments) {
      recoverPayment(payment);
    }
  }

  private void recoverPayment(Payment payment) {
    String transactionKey = payment.getTransactionKey();

    try {
      PgTransactionResponse response = pgClient.getTransaction(
          String.valueOf(payment.getUserId()),
          transactionKey
      );

      if (response.isPending()) {
        log.debug("[스케줄러] 결제 진행중 . transactionKey={}", transactionKey);
        return;
      }

      if (response.isSuccess()) {
        paymentCallbackFacade.handleSuccess(transactionKey);
        log.info("[스케줄러]복구: 결제 성공 처리. transactionKey={}", transactionKey);
        return;
      }

      if (response.isFailed()) {
        paymentCallbackFacade.handleFailed(transactionKey, response.reason());
        log.info("[스케줄러]복구: 결제 실패 처리. transactionKey={}, reason={}",
            transactionKey, response.reason());
        return;
      }

      log.warn("[스케줄러] 알 수 없는 상태. transactionKey={}, status={}", transactionKey, response.status());
    } catch (Exception e) {
      log.error("[스케줄러] 복구 실패. transactionKey={}", transactionKey, e);
    }
  }
}
