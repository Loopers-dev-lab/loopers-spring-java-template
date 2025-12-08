package com.loopers.infrastructure.scheduler;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PROCESSING 상태인 결제에 대해 주기적으로 상태를 확인하는 스케줄러
 * - 1분 주기로 실행
 * - 콜백이 오지 않은 결제에 대해 PG사에 상태 확인 요청
 * - 각 결제는 별도 트랜잭션으로 처리되어 부분 실패 시에도 성공한 결제는 커밋됨
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentStatusCheckScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusCheckService paymentStatusCheckService;

    // 최대 확인 횟수 (10회까지 재시도)
    private static final int MAX_CHECK_COUNT = 10;
    // 확인 주기 (1분)
    private static final int CHECK_INTERVAL_MINUTES = 1;

    /**
     * 1분마다 PROCESSING 상태인 결제 확인
     * - 전체 배치에 트랜잭션 적용하지 않음
     * - 각 결제는 PaymentStatusCheckService에서 독립적인 트랜잭션으로 처리
     */
    @Scheduled(fixedRate = 60000) // 60초 = 1분
    public void checkProcessingPayments() {
        log.info("[결제 상태 확인 스케줄러] 결제 상태 확인 시작");

        try {
            LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(CHECK_INTERVAL_MINUTES);

            // 확인 대상 결제 조회
            List<Payment> processingPayments = paymentRepository.findProcessingPaymentsForStatusCheck(
                    thresholdTime,
                    MAX_CHECK_COUNT
            );

            if (processingPayments.isEmpty()) {
                log.info("[결제 상태 확인 스케줄러] 확인할 처리 중인 결제가 없습니다");
                return;
            }

            log.info("[결제 상태 확인 스케줄러] 확인할 처리 중인 결제 {}건 발견",
                    processingPayments.size());

            // 각 결제를 독립적인 트랜잭션으로 처리
            for (Payment payment : processingPayments) {
                paymentStatusCheckService.checkPaymentStatus(payment.getPaymentId());
            }

            log.info("[결제 상태 확인 스케줄러] 결제 상태 확인 완료");
        } catch (Exception e) {
            log.error("[결제 상태 확인 스케줄러] 결제 상태 확인 중 오류 발생", e);
        }
    }
}