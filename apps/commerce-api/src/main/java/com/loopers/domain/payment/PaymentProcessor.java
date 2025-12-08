package com.loopers.domain.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.stereotype.Component;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.payment.client.dto.PgPaymentRequest;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 서비스 (Domain Service)
 *
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final PaymentService paymentService;
    private final UserService userService;
    private final PaymentValidator paymentValidator;
    private final PgGateway pgGateway;

    /**
     * PG 카드 결제 처리
     *
     * @param user    사용자 엔티티
     * @param command 결제 명령
     * @return 생성된 결제 엔티티
     */
    @Retry(name = "pgClient", fallbackMethod = "processPaymentFallback")
    @CircuitBreaker(name = "pgClient", fallbackMethod = "processPaymentFallback")
    public PaymentEntity processPgPayment(UserEntity user, PaymentCommand command) {
        log.info("PG 결제 처리 시작 - orderNumber: {}, username: {}, amount: {}",
                command.orderId(), user.getUsername(), command.amount());

        // 1. PENDING 상태 결제 생성
        PaymentEntity pendingPayment = paymentService.createPending(user, command);

        // 2. PG 결제 요청
        PgPaymentResponse pgResponse = pgGateway.requestPayment(
                user.getUsername(),
                PgPaymentRequest.of(command)
        );

        // 3. PG 응답 검증
        paymentValidator.validatePgResponse(pgResponse);

        // 4. transactionKey 업데이트
        pendingPayment.updateTransactionKey(pgResponse.transactionKey());

        log.info("PG 결제 요청 완료 - orderNumber: {}, transactionKey: {}, status: {}, 콜백 대기 중",
                command.orderId(), pgResponse.transactionKey(), pgResponse.status());

        return pendingPayment;
    }

    /**
     * PG에서 결제 상태 조회 (보안 강화)
     * <p>
     * 콜백 데이터만 신뢰하지 않고, PG API에 직접 조회하여 검증
     *
     * @param transactionKey PG 거래 키
     * @param username       사용자명
     * @return PG 실제 결제 데이터
     */
    public PgPaymentResponse verifyPaymentFromPg(String transactionKey, String username) {
        log.debug("PG 결제 상태 조회 시작 - transactionKey: {}", transactionKey);

        try {
            PgPaymentResponse pgData = pgGateway.getPayment(username, transactionKey);

            if (pgData.isApiFail()) {
                throw new RuntimeException(
                        String.format("PG 결제 조회 실패 - errorCode: %s, message: %s",
                                pgData.meta().errorCode(), pgData.meta().message())
                );
            }

            log.debug("PG 결제 상태 조회 완료 - transactionKey: {}, status: {}",
                    transactionKey, pgData.status());

            return pgData;

        } catch (Exception e) {
            log.error("PG 결제 상태 조회 중 오류 발생 - transactionKey: {}", transactionKey, e);
            throw new CoreException(ErrorType.PG_API_FAIL, "PG 결제 상태 조회에 실패했습니다.");
        }
    }

    /**
     * Fallback 메서드
     * <p>
     * PG 서비스 장애 또는 타임아웃(500ms) 시 실패 결제 생성
     */
    @SuppressWarnings("unused")
    private PaymentEntity processPaymentFallback(UserEntity user, PaymentCommand command, Throwable t) {
        log.error("PG 서비스 장애 또는 타임아웃, 결제 요청 실패 처리 - exception: {}, message: {}",
                t.getClass().getSimpleName(), t.getMessage(), t);

        return paymentService.createFailedPayment(user,
                command,
                "결제 시스템 응답 지연으로 처리되지 않았습니다. 다시 시도해 주세요."
        );
    }
}

