package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncBatchService {

  private final PaymentService paymentService;
  private final PgClient pgClient;

  public void syncPendingPayments() {
    log.info("결제 상태 동기화 배치 시작");

    List<Payment> pendingPayments = paymentService.getPendingPayments();
    log.info("동기화 대상 결제 건수: {}", pendingPayments.size());

    int successCount = 0;
    int failCount = 0;

    for (Payment payment : pendingPayments) {
      try {
        syncPaymentStatus(payment);
        successCount++;
      } catch (Exception e) {
        log.error("결제 동기화 실패 - paymentId: {}, orderId: {}, error: {}",
            payment.getId(), payment.getOrderId(), e.getMessage());
        failCount++;
      }
    }

    log.info("결제 상태 동기화 배치 완료 - 성공: {}, 실패: {}", successCount, failCount);
  }

  private void syncPaymentStatus(Payment payment) {
    if (payment.getTransactionKey() == null) {
      log.debug("transactionKey가 없는 결제는 스킵 - paymentId: {}", payment.getId());
      return;
    }

    try {
      PgPaymentInfoResponse pgInfo = pgClient.getPaymentInfo(payment.getTransactionKey());

      PaymentStatus pgStatus = PaymentStatus.valueOf(pgInfo.status());
      if (payment.getStatus() != pgStatus) {
        log.info("결제 상태 변경 감지 - paymentId: {}, {} -> {}",
            payment.getId(), payment.getStatus(), pgStatus);

        paymentService.updatePaymentFromCallback(payment);
      }
    } catch (Exception e) {
      log.warn("PG 결제 정보 조회 실패 - paymentId: {}, transactionKey: {}",
          payment.getId(), payment.getTransactionKey(), e);
    }
  }

  public void syncFailedPayments() {
    log.info("실패 결제 재확인 배치 시작");

    List<Payment> failedPayments = paymentService.getRecentFailedPayments(24);
    log.info("재확인 대상 실패 결제 건수: {}", failedPayments.size());

    for (Payment payment : failedPayments) {
      try {
        syncPaymentStatus(payment);
      } catch (Exception e) {
        log.error("실패 결제 재확인 실패 - paymentId: {}", payment.getId(), e);
      }
    }

    log.info("실패 결제 재확인 배치 완료");
  }
}
