package com.loopers.infrastructure.payment;

import com.loopers.domain.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentType;
import com.loopers.infrastructure.external.feign.PaymentClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * FeignPaymentGateway 단위 테스트
 * Circuit Breaker 동작을 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class FeignPaymentGatewayTest {

    private static final String TEST_USER_ID = "user123";
    private static final String TEST_CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";
    private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(10000);
    private static final String CIRCUIT_BREAKER_NAME = "paymentGateway";

    // Circuit Breaker 설정 상수
    private static final int SLIDING_WINDOW_SIZE = 10;
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final float FAILURE_RATE_THRESHOLD = 50f;
    private static final int WAIT_DURATION_SECONDS = 10;
    private static final int PERMITTED_CALLS_IN_HALF_OPEN = 2;

    private FeignPaymentGateway paymentGateway;
    private CircuitBreaker circuitBreaker;

    @Mock
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        circuitBreaker = createCircuitBreaker();
        paymentGateway = new FeignPaymentGateway(paymentClient);
    }

    @DisplayName("연속 실패 시 Circuit Breaker가 OPEN 상태로 전환된다")
    @Test
    void shouldOpenCircuitBreakerAfterConsecutiveFailures() {
        // given
        given(paymentClient.payment(any()))
                .willThrow(new RuntimeException("Payment service unavailable"));

        // when
        int failureCount = executePaymentsAndCountFailures(MINIMUM_NUMBER_OF_CALLS);

        // then
        assertThat(failureCount).isEqualTo(MINIMUM_NUMBER_OF_CALLS);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(MINIMUM_NUMBER_OF_CALLS);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(MINIMUM_NUMBER_OF_CALLS);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(100.0f);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    /**
     * Circuit Breaker 설정 생성
     */
    private CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_SECONDS))
                .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN)
                .build();

        return CircuitBreaker.of(CIRCUIT_BREAKER_NAME, config);
    }

    /**
     * 결제 요청을 여러 번 시도하고 실패 횟수를 반환
     */
    private int executePaymentsAndCountFailures(int attempts) {
        int failureCount = 0;

        for (int i = 0; i < attempts; i++) {
            try {
                circuitBreaker.executeSupplier(() ->
                        paymentGateway.processPayment(
                                TEST_USER_ID,
                                createTestPayment(),
                                TEST_CALLBACK_URL)
                );
            } catch (Exception e) {
                failureCount++;
            }
        }

        return failureCount;
    }

    /**
     * 테스트용 Payment 객체 생성
     */
    private Payment createTestPayment() {
        return Payment.createPaymentForCard(
                new Order(),
                Money.of(TEST_AMOUNT),
                PaymentType.CARD,
                CardType.SAMSUNG,
                "1234-5678-9012-3456"
        );
    }
}
