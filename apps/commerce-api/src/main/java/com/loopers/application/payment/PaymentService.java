package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.PgClient;
import com.loopers.infrastructure.payment.PgPaymentRequest;
import com.loopers.infrastructure.payment.PgPaymentResponse;
import com.loopers.infrastructure.payment.PgTransactionDetail;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PgClient pgClient;
    private final PaymentRepository paymentRepository;

    @Retry(name = "pgRetry")
    @CircuitBreaker(name = "pgCircuit")
    @Transactional
    public Payment requestPayment(String userId, String orderId, BigDecimal amount,
        String cardType, String cardNo) {
        log.info("결제 요청 시작 - orderId: {}, userId: {}, amount: {}", orderId, userId, amount);

        PgPaymentRequest request = PgPaymentRequest.builder()
            .orderId(orderId)
            .cardType(cardType)
            .cardNo(cardNo)
            .amount(amount.longValue())
            .callbackUrl("http://localhost:8080/api/v1/payments/callback")
            .build();

        try {
            PgPaymentResponse response = pgClient.requestPayment(userId, request);

            Payment payment = Payment.builder()
                .transactionKey(response.transactionKey())
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(mapStatus(response.status()))
                .cardType(cardType)
                .cardNo(cardNo)
                .build();

            if (payment.isFailed()) {
                payment.updateStatus(PaymentStatus.FAILED, response.reason());
            }

            return paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("PG 결제 요청 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public Payment createFallbackPayment(String userId, String orderId, BigDecimal amount,
        String cardType, String cardNo) {
        log.warn("Fallback 결제 생성 - orderId: {}, userId: {}", orderId, userId);

        Payment payment = Payment.builder()
            .transactionKey("TEMP-" + System.currentTimeMillis())
            .orderId(orderId)
            .userId(userId)
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .cardType(cardType)
            .cardNo(cardNo)
            .build();

        return paymentRepository.save(payment);
    }

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getPaymentDetailFallback")
    @Transactional(readOnly = true)
    public PgTransactionDetail getPaymentDetail(String userId, String transactionKey) {
        log.info("결제 상태 조회 - transactionKey: {}", transactionKey);
        return pgClient.getPaymentDetail(userId, transactionKey);
    }

    public PgTransactionDetail getPaymentDetailFallback(String userId, String transactionKey,
        Throwable t) {
        log.error("결제 상태 조회 Fallback 실행 - transactionKey: {}, error: {}",
            transactionKey, t.getMessage());
        return new PgTransactionDetail(transactionKey, "", "", "", 0L, "PENDING", null);
    }

    @Transactional
    public void updatePaymentStatus(String transactionKey, String status, String reason) {
        Payment payment = paymentRepository.findByTransactionKey(transactionKey)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "결제 정보를 찾을 수 없습니다."));

        PaymentStatus paymentStatus = mapStatus(status);
        payment.updateStatus(paymentStatus, reason);

        log.info("결제 상태 업데이트 - transactionKey: {}, status: {}", transactionKey,
            paymentStatus);
    }

    @Transactional
    public void syncPaymentStatus(String userId, String transactionKey) {
        try {
            PgTransactionDetail detail = getPaymentDetail(userId, transactionKey);

            Payment payment = paymentRepository.findByTransactionKey(transactionKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                    "결제 정보를 찾을 수 없습니다."));

            PaymentStatus newStatus = mapStatus(detail.status());
            if (payment.getStatus() != newStatus) {
                payment.updateStatus(newStatus, detail.reason());
                log.info("결제 상태 동기화 완료 - transactionKey: {}, status: {} -> {}",
                    transactionKey, payment.getStatus(), newStatus);
            }
        } catch (Exception e) {
            log.error("결제 상태 동기화 실패 - transactionKey: {}, error: {}",
                transactionKey, e.getMessage());
        }
    }

    private PaymentStatus mapStatus(String status) {
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> PaymentStatus.SUCCESS;
            case "FAILED" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }
}
