package com.loopers.domain.payment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.payment.PaymentCommand;
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
     * <p>
     * 도메인 이벤트는 PaymentEntity.processCallbackResult()에서 발행됨
     */
    @Transactional
    public void processPaymentResult(PaymentEntity payment, PaymentV1Dtos.PgCallbackRequest request) {
        // 엔티티에 콜백 결과 처리 위임 (도메인 이벤트 발행 포함)
        payment.processCallbackResult(request.status(), request.reason());

        // 변경사항 저장
        paymentRepository.save(payment);

        // 로그 기록
        logPaymentResult(request);
    }

    /**
     * 결제 결과 로깅
     */
    private void logPaymentResult(PaymentV1Dtos.PgCallbackRequest request) {
        switch (request.status()) {
            case "SUCCESS" -> log.info("결제 성공 처리 완료 - transactionKey: {}, orderNumber: {}",
                    request.transactionKey(), request.orderId());
            case "FAILED" -> log.warn("결제 실패 처리 - transactionKey: {}, reason: {}",
                    request.transactionKey(), request.reason());
            case "PENDING" -> log.debug("결제 처리 중 상태 콜백 수신 - transactionKey: {}",
                    request.transactionKey());
            default -> log.error("알 수 없는 결제 상태 - transactionKey: {}, status: {}",
                    request.transactionKey(), request.status());
        }
    }

    @Transactional
    public PaymentEntity upsertFailPayment(UserEntity user, PaymentCommand command, String reason) {
        Optional<PaymentEntity> existingPayment = paymentRepository.findByOrderNumber(command.orderNumber());

        if (existingPayment.isPresent()) {
            PaymentEntity payment = existingPayment.get();
            payment.fail(reason);
            return paymentRepository.save(payment);
        } else {
            PaymentEntity newPayment = PaymentEntity.createFailed(user, command, reason);
            return paymentRepository.save(newPayment);
        }
    }
}
