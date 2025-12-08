package com.loopers.application.payment;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 타임아웃 처리 스케줄러
 * <p>
 * PENDING 상태로 오래 대기 중인 결제 건을 TIMEOUT 처리합니다.
 * <p>
 * 설계 원칙:
 * - 스케줄러 메서드는 트랜잭션 없음 (조회 및 반복만 수행)
 * - 개별 결제 건마다 독립적인 트랜잭션으로 처리 (PaymentTimeoutProcessor 사용)
 * - 부분 실패 허용 (한 건 실패해도 나머지 건 계속 처리)
 *
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private final PaymentService paymentService;
    private final PaymentTimeoutProcessor timeoutProcessor;

    /**
     * 5분마다 PENDING 상태로 10분 이상 대기 중인 결제 건을 TIMEOUT 처리
     * <p>
     * 트랜잭션 없음: 조회 및 반복만 수행하여 롱 트랜잭션 방지
     */
    @Scheduled(fixedDelay = 300000) // 5분 (300,000ms)
    public void handleTimeoutPayments() {
        ZonedDateTime timeoutThreshold = ZonedDateTime.now().minusMinutes(10);

        List<PaymentEntity> timeoutPayments =
                paymentService.findPendingPaymentsOlderThan(timeoutThreshold);

        if (timeoutPayments.isEmpty()) {
            return;
        }

        log.warn("결제 타임아웃 처리 시작 - 대상 건수: {}", timeoutPayments.size());

        int successCount = 0;
        int failureCount = 0;

        for (PaymentEntity payment : timeoutPayments) {
            boolean success = timeoutProcessor.processTimeout(payment);
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        log.info("결제 타임아웃 처리 완료 - 총 {}건 (성공: {}건, 실패: {}건)",
                timeoutPayments.size(), successCount, failureCount);
    }
}
