package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.external.dto.PaymentExternalDto;
import com.loopers.infrastructure.external.feign.PaymentClient;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignPaymentGateway implements PaymentGateway {

    private final PaymentClient paymentClient;

    @Override
    @Retry(name = "paymentGateway")
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
    public PaymentResult processPayment(String userId, Payment payment, String callbackUrl) {
        log.debug("결제 요청 - paymentId: {}, userId: {}", payment.getPaymentId(), userId);

        // Infrastructure DTO로 변환
        PaymentExternalDto.PaymentRequest request = PaymentExternalDto.PaymentRequest.from(payment, callbackUrl);

        // 외부 결제 시스템 호출
        PaymentExternalDto.PaymentResponse response = paymentClient.payment(request);

        // 실패 응답 처리
        if (!response.isSuccess()) {
            throw new CoreException(
                    ErrorType.PAYMENT_REQUEST_FAILED,
                    "결제 요청 실패: " + response.getErrorMessage()
            );
        }

        // Domain 모델로 변환하여 반환
        return new PaymentResult(
                response.getTransactionKey(),  // transactionKey 사용
                response.getStatus(),          // "PENDING"
                "결제 요청이 접수되었습니다"
        );
    }

    @Override
    @Retry(name = "paymentGateway")
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "checkPaymentStatusFallback")
    public PaymentResult checkPaymentStatus(String pgTransactionId) {
        log.debug("결제 상태 조회 - pgTransactionId: {}", pgTransactionId);

        // 외부 결제 시스템에 상태 확인 요청
        PaymentExternalDto.PaymentResponse response = paymentClient.paymentInfo(pgTransactionId);

        // 실패 응답 처리
        if (!response.isSuccess()) {
            throw new CoreException(
                    ErrorType.PAYMENT_REQUEST_FAILED,
                    "결제 상태 조회 실패: " + response.getErrorMessage()
            );
        }

        // Domain 모델로 변환하여 반환
        return new PaymentResult(
                response.getTransactionKey(),
                response.getStatus(),
                response.getErrorMessage() != null ? response.getErrorMessage() : "상태 조회 완료"
        );
    }

    /**
     * Payment Fallback 메서드
     * Circuit이 Open 상태이거나, 재시도 실패 시 호출
     * */
    private PaymentResult paymentFallback(String userId, Payment payment, String callbackUrl, Exception ex) {
        log.error("결제 시스템 장애 발생 - userId: {}, paymentKey: {}, error: {}",
                userId, payment.getPaymentId(), ex.getMessage(), ex);

        // 방법 1 : 결제 대기 상태 처리 ( 현재 시스템에는 방법 2의 처리가 구현되어 있지 않아 방법 1로 처리함.)
        return new PaymentResult(
                null,
                "FAIL",
                "결제 시스템 장애로 결제 대기 상태로 변경되었습니다."
        );

        // 방법 2 : 다른 PG사에 요청을 보낸다.
    }

    /**
     * Check Payment Status Fallback 메서드
     * Circuit이 Open 상태이거나, 재시도 실패 시 호출
     * */
    private PaymentResult checkPaymentStatusFallback(String pgTransactionId, Exception ex) {
        log.error("결제 상태 조회 시스템 장애 발생 - pgTransactionId: {}, error: {}",
                pgTransactionId, ex.getMessage(), ex);

        // 상태 조회 실패 시 PROCESSING 유지
        return new PaymentResult(
                pgTransactionId,
                "PROCESSING",
                "결제 시스템 장애로 상태 조회에 실패했습니다."
        );
    }
}
