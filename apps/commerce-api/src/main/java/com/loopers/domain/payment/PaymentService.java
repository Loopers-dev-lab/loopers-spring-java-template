package com.loopers.domain.payment;

import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PgClient pgClient;

    @Transactional
    public Payment createPgCardPayment(Long orderId, Long userId, String cardType, String cardNo,
                                       Long amount, String callbackUrl, String idempotencyKey) {
        Payment payment = Payment.createForPgCard(
                orderId, userId, cardType, cardNo, amount, callbackUrl, idempotencyKey
        );
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment createPointPayment(Long orderId, Long userId, Long amount, String idempotencyKey) {
        Payment payment = Payment.createForPoint(orderId, userId, amount, idempotencyKey);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "주문에 대한 결제 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "거래 ID에 해당하는 결제 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional
    public void updatePaymentStatus(Payment payment, String status) {
        switch (status) {
            case "SUCCESS" -> payment.markAsSuccess();
            case "FAILED" -> payment.markAsFailed("결제 실패");
            case "LIMIT_EXCEEDED" -> payment.markAsLimitExceeded();
            case "INVALID_CARD" -> payment.markAsInvalidCard();
            case "PG_TIMEOUT" -> payment.markAsTimeout();
            case "CALLBACK_TIMEOUT" -> payment.markAsCallbackTimeout();
            default -> throw new CoreException(ErrorType.BAD_REQUEST, "알 수 없는 결제 상태입니다: " + status);
        }
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPendingPayments() {
        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }
}
