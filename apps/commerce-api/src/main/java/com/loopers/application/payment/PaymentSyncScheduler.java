package com.loopers.application.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSyncScheduler {

  private final PaymentSyncBatchService batchService;

  @Scheduled(fixedDelay = 300000) // 5분마다 실행
  public void syncPendingPayments() {
    try {
      batchService.syncPendingPayments();
    } catch (Exception e) {
      log.error("결제 상태 동기화 배치 실행 중 오류 발생", e);
    }
  }

  @Scheduled(cron = "0 */30 * * * *") // 30분마다 실행
  public void syncFailedPayments() {
    try {
      batchService.syncFailedPayments();
    } catch (Exception e) {
      log.error("실패 결제 재확인 배치 실행 중 오류 발생", e);
    }
  }
}