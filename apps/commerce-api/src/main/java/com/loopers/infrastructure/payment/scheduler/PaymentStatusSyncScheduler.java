package com.loopers.infrastructure.payment.scheduler;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 결제 상태 동기화 스케줄러
 * - PENDING 상태인 결제를 주기적으로 확인
 * - PG에 상태 조회하여 동기화
 * - 콜백이 오지 않은 결제 처리
 */
@Component
@RequiredArgsConstructor
public class PaymentStatusSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusSyncScheduler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentFacade paymentFacade;

    /**
     * PENDING 결제 상태 동기화
     * - 매 30초마다 실행
     * - PENDING 상태인 결제를 조회하여 PG 상태와 동기화
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000) // 30초마다, 초기 지연 10초
    public void syncPendingPayments() {
        try {
            List<Payment> pendingPayments = paymentRepository.findPendingPayments();

            if (pendingPayments.isEmpty()) {
                return;
            }

            log.info("PENDING 결제 상태 동기화 시작: count={}", pendingPayments.size());

            int successCount = 0;
            int failCount = 0;

            for (Payment payment : pendingPayments) {
                try {
                    paymentFacade.syncPaymentStatus(payment.getId());
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("결제 상태 동기화 실패: paymentId={}, error={}", payment.getId(), e.getMessage());
                }
            }

            log.info("PENDING 결제 상태 동기화 완료: total={}, success={}, fail={}",
                    pendingPayments.size(), successCount, failCount);
        } catch (Exception e) {
            log.error("결제 상태 동기화 스케줄러 실행 중 예외 발생", e);
        }
    }
}
