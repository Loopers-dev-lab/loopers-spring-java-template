package com.loopers.domain.payment;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.user.UserEntity;
import com.loopers.interfaces.api.payment.PaymentV1Dtos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentEntity createPending(UserEntity user, PaymentCommand command) {
        PaymentEntity pending = PaymentEntity.createPending(
                user, command
        );
        return paymentRepository.save(pending);
    }

    @Transactional
    public PaymentEntity createFailedPayment(UserEntity user, PaymentCommand command, String reason) {
        PaymentEntity pending = PaymentEntity.createFailed(
                user, command, reason
        );
        return paymentRepository.save(pending);
    }

    @Transactional(readOnly = true)
    public PaymentEntity getByTransactionKey(String transactionKey) {
        return paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다"));
    }

    /**
     * PENDING 상태로 특정 시간 이전에 생성된 결제 건 조회
     *
     * @param threshold 기준 시간
     * @return PENDING 상태의 오래된 결제 건 목록
     */
    @Transactional(readOnly = true)
    public List<PaymentEntity> findPendingPaymentsOlderThan(ZonedDateTime threshold) {
        return paymentRepository.findPendingPaymentsOlderThan(threshold);
    }

    /**
     * 결제 결과 처리
     * <p>
     * 결제 상태에 따라:
     * - SUCCESS: 결제 완료 처리 + PaymentCompletedEvent 발행
     * - FAILED: 결제 실패 처리 + PaymentFailedEvent 발행
     * - PENDING: 무시 (아직 처리 중)
     */
    public void processPaymentResult(PaymentEntity payment, PaymentV1Dtos.PgCallbackRequest request) {
        switch (request.status()) {
            case "SUCCESS" -> {
                payment.complete();
                log.info("결제 성공 처리 완료 - transactionKey: {}, orderNumber: {}",
                        request.transactionKey(), request.orderId());

                eventPublisher.publishEvent(new PaymentCompletedEvent(
                        payment.getTransactionKey(),
                        payment.getOrderNumber(),
                        payment.getUserId(),
                        payment.getAmount()
                ));
            }
            case "FAILED" -> {
                payment.fail(request.reason());
                log.warn("결제 실패 처리 - transactionKey: {}, reason: {}",
                        request.transactionKey(), request.reason());

                eventPublisher.publishEvent(new PaymentFailedEvent(
                        payment.getTransactionKey(),
                        payment.getOrderNumber(),
                        payment.getUserId(),
                        request.reason()
                ));
            }
            case "PENDING" -> log.debug("결제 처리 중 상태 콜백 수신 - transactionKey: {}", request.transactionKey());
            default -> log.error("알 수 없는 결제 상태 - transactionKey: {}, status: {}",
                    request.transactionKey(), request.status());
        }
    }
}
