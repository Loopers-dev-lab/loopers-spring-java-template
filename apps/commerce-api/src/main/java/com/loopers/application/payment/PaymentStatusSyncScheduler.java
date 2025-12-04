package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusSyncScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void syncPendingPayments() {
        log.info("PENDING 상태 결제 동기화 시작");

        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

        if (pendingPayments.isEmpty()) {
            log.info("동기화할 PENDING 결제 없음");
            return;
        }

        log.info("동기화할 PENDING 결제 수: {}", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                // TEMP로 시작하는 transactionKey는 fallback으로 생성된 것이므로 건너뜀
                if (payment.getTransactionKey().startsWith("TEMP-")) {
                    log.warn("Fallback으로 생성된 결제 건너뜀 - transactionKey: {}",
                        payment.getTransactionKey());
                    continue;
                }

                paymentService.syncPaymentStatus(payment.getUserId(),
                    payment.getTransactionKey());
            } catch (Exception e) {
                log.error("결제 상태 동기화 중 오류 발생 - transactionKey: {}, error: {}",
                    payment.getTransactionKey(), e.getMessage());
            }
        }

        log.info("PENDING 상태 결제 동기화 완료");
    }
}
