package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSyncScheduler {
    private final PaymentRepository paymentRepository;
    private final PgClient pgClient;
    private final PaymentFacade paymentFacade;

    // 30초마다 PENDING 결제 동기화
    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    public void syncPendingPayments() {
        List<Payment> pendings = paymentRepository.findPending(100);
        if (pendings.isEmpty()) {
            return;
        }
        for (Payment payment : pendings) {
            try {
                PgDto.Response res = pgClient.findByOrderId(payment.getUserId(), payment.getOrderId());
                if (res == null || res.status() == null) {
                    continue;
                }
                // 상태 변경: PENDING -> SUCCESS/FAILED
                String statusName = res.status();
                String reason = res.reason();
                if (!PaymentStatus.PENDING.name().equalsIgnoreCase(statusName)) {
                    paymentFacade.updatePaymentStatus(payment.getTransactionKey(), statusName.toUpperCase(), reason);
                }
            } catch (Exception e) {
                log.warn("결제 동기화 실패 - txKey: {}, orderId: {}, error: {}", payment.getTransactionKey(), payment.getOrderId(), e.getMessage());
            }
        }
    }
}










