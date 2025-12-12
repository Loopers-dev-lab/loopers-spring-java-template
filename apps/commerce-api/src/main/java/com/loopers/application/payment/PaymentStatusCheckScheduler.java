package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusCheckScheduler {

    private final PaymentService paymentService;
    private final PaymentCallbackService paymentCallbackService;
    private final PgService pgService;

    @Scheduled(fixedDelayString = "30000")
    public void checkPendingPayments() {
        log.debug("결제 상태 확인 스케줄러 시작");

        List<Payment> pendingPayments = paymentService.getAllPendingPayments();

        if (pendingPayments.isEmpty()) {
            log.debug("확인할 PENDING 결제 없음");
            return;
        }

        log.info("PENDING 결제 {}건 확인 시작", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                checkAndUpdatePaymentStatus(payment);
            } catch (Exception e) {
                log.error("결제 상태 확인 실패: paymentId={}, transactionId={}",
                        payment.getId(), payment.getTransactionId(), e);
            }
        }
    }

    private void checkAndUpdatePaymentStatus(Payment payment) {
        if (payment.getTransactionId() == null) {
            log.warn("거래 ID가 없는 결제: paymentId={}", payment.getId());

            PaymentCallbackDto timeoutCallback = new PaymentCallbackDto(
                    "TIMEOUT-" + payment.getId(),
                    "PG_TIMEOUT",
                    "거래 ID 생성 실패로 인한 타임아웃"
            );
            paymentCallbackService.processCallback(timeoutCallback);
            return;
        }

        String userId = payment.getUserId().toString();

        // PG 시스템에서 결제 상태 조회
        PgPaymentResponse pgResponse = pgService.getPaymentDetail(userId, payment.getTransactionId());

        if (pgResponse == null) {
            log.warn("PG 응답 없음 (Fallback): transactionId={}", payment.getTransactionId());

            PaymentCallbackDto timeoutCallback = new PaymentCallbackDto(
                    payment.getTransactionId(),
                    "CALLBACK_TIMEOUT",
                    "PG 시스템 응답 없음 (콜백 미수신)"
            );
            paymentCallbackService.processCallback(timeoutCallback);
            return;
        }

        log.info("PG 결제 상태 확인 결과: transactionId={}, status={}",
                payment.getTransactionId(), pgResponse.status());

        // 콜백 처리
        PaymentCallbackDto callback = new PaymentCallbackDto(
                pgResponse.transactionId(),
                pgResponse.status(),
                pgResponse.message()
        );

        paymentCallbackService.processCallback(callback);
    }
}
