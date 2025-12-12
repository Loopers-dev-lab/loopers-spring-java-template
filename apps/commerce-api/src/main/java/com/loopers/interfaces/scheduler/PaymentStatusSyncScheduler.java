package com.loopers.interfaces.scheduler;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusSyncScheduler {

    private final PaymentFacade paymentFacade;
    private final PaymentService paymentService;

    @Scheduled(cron = "0 */5 * * * *")
    public void syncPendingPayments() {
        log.info("PENDING 상태 Payment 동기화 스케줄러 시작");

        ZonedDateTime thirtyMinutesAgo = ZonedDateTime.now().minusMinutes(30);
        List<Payment> pendingPayments = paymentService.getPendingPaymentsCreatedBefore(thirtyMinutesAgo);

        int successCount = 0;
        int failureCount = 0;

        for (Payment payment : pendingPayments) {
            if (syncPayment(payment)) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        log.info("PENDING 상태 Payment 동기화 완료: 전체={}, 성공={}, 실패={}",
                pendingPayments.size(), successCount, failureCount);
    }

    private boolean syncPayment(Payment payment) {
        try {
            paymentFacade.syncPaymentStatus(payment);
            return true;
        } catch (Exception e) {
            log.error("Payment 상태 동기화 실패: orderKey={}, transactionKey={}, error={}",
                    payment.getOrderKey(), payment.getTransactionKey(), e.getMessage(), e);
            return false;
        }
    }
}

