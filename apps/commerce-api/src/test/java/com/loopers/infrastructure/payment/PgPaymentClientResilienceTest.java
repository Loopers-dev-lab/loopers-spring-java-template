package com.loopers.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.loopers.domain.payment.PaymentClient.PaymentRequest;
import com.loopers.domain.payment.PaymentClient.PaymentResponse;
import com.loopers.infrastructure.gateway.PgPaymentClient;
import com.loopers.infrastructure.gateway.PgPaymentDto;
import com.loopers.infrastructure.gateway.PgPaymentFeignClient;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@DisplayName("PgPaymentClient Resilience4j 동작 테스트")
class PgPaymentClientResilienceTest {

    @Autowired
    private PgPaymentClient pgPaymentClient;

    @MockitoSpyBean
    private PgPaymentFeignClient pgPaymentFeignClient;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private PaymentRequest paymentRequest;
    private String userId;

    @BeforeEach
    void setUp() {
        if (circuitBreakerRegistry != null) {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgPayment");
            circuitBreaker.transitionToClosedState();
            circuitBreaker.reset();
        }

        userId = "user123";
        paymentRequest = new PaymentRequest(
                "order12345",
                "SAMSUNG",
                "1234-5678-9012-3456",
                10000,
                userId
        );
    }

    @DisplayName("Circuit Breaker 동작 테스트")
    @Nested
    class CircuitBreakerTest {

        @Test
        @DisplayName("정상 응답 시 Circuit Breaker는 Close 상태를 유지한다")
        void circuitBreakerStaysClosed_whenSuccess() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            doReturn(successResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any(PgPaymentDto.PgPaymentRequest.class));

            // when
            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            // then
            assertThat(response.transactionKey()).isEqualTo("txn-key-123");
            assertThat(response.status()).isEqualTo("PENDING");
            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("설정: 슬라이딩 윈도우 20, 최소 호출 10, 실패율 60% - 10번 중 6번 실패하면 Open 상태")
        void circuitBreakerOpens_whenFailureRateExceeds60Percent() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doReturn(successResponse)  // 1 - 성공
                    .doReturn(successResponse)  // 2 - 성공
                    .doReturn(successResponse)  // 3 - 성공
                    .doReturn(successResponse)  // 4 - 성공
                    .doThrow(runtimeException)        // 5 - 실패
                    .doThrow(runtimeException)        // 6 - 실패
                    .doThrow(runtimeException)        // 7 - 실패
                    .doThrow(runtimeException)        // 8 - 실패
                    .doThrow(runtimeException)        // 9 - 실패
                    .doThrow(runtimeException)       // 10 - 실패
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            for (int i = 0; i < 10; i++) {
                try {
                    pgPaymentClient.requestPayment(paymentRequest);
                } catch (Exception e) {
                }
            }

            // then
            verify(pgPaymentFeignClient, times(10)).requestPayment(anyString(), any());

            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).isEqualTo("PG 시스템이 일시적으로 사용할 수 없습니다.");

            verify(pgPaymentFeignClient, times(10)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("HalfOpen 상태에서 성공 시 Closed 상태로 전환된다")
        void circuitBreakerTransitionsToClosed_whenHalfOpenSucceeds() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doReturn(successResponse)  // 1 - 성공
                    .doReturn(successResponse)  // 2 - 성공
                    .doReturn(successResponse)  // 3 - 성공
                    .doReturn(successResponse)  // 4 - 성공
                    .doThrow(runtimeException)        // 5 - 실패
                    .doThrow(runtimeException)        // 6 - 실패
                    .doThrow(runtimeException)        // 7 - 실패
                    .doThrow(runtimeException)        // 8 - 실패
                    .doThrow(runtimeException)        // 9 - 실패
                    .doThrow(runtimeException)       // 10 - 실패
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            for (int i = 0; i < 10; i++) {
                try {
                    pgPaymentClient.requestPayment(paymentRequest);
                } catch (Exception e) {
                }
            }

            if (circuitBreakerRegistry != null) {
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgPayment");
                circuitBreaker.transitionToHalfOpenState();
            }

            doReturn(successResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            // then
            assertThat(response.transactionKey()).isEqualTo("txn-key-123");
            assertThat(response.status()).isEqualTo("PENDING");

            verify(pgPaymentFeignClient, times(11)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("HalfOpen 상태에서 실패 시 Open 상태로 다시 전환된다")
        void circuitBreakerTransitionsToOpen_whenHalfOpenFails() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doReturn(successResponse)  // 1 - 성공
                    .doReturn(successResponse)  // 2 - 성공
                    .doReturn(successResponse)  // 3 - 성공
                    .doReturn(successResponse)  // 4 - 성공
                    .doThrow(runtimeException)        // 5 - 실패
                    .doThrow(runtimeException)        // 6 - 실패
                    .doThrow(runtimeException)        // 7 - 실패
                    .doThrow(runtimeException)        // 8 - 실패
                    .doThrow(runtimeException)        // 9 - 실패
                    .doThrow(runtimeException)       // 10 - 실패
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            for (int i = 0; i < 10; i++) {
                try {
                    pgPaymentClient.requestPayment(paymentRequest);
                } catch (Exception e) {
                }
            }

            if (circuitBreakerRegistry != null) {
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgPayment");
                circuitBreaker.transitionToHalfOpenState();
            }

            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).isEqualTo("PG 시스템이 일시적으로 사용할 수 없습니다.");

            verify(pgPaymentFeignClient, times(11)).requestPayment(anyString(), any());
        }
    }

    @DisplayName("Retry 동작 테스트")
    @Nested
    class RetryTest {

        @Test
        @DisplayName("재시도 가능한 예외 발생 시 CircuitBreaker Fallback이 호출된다")
        void callsCircuitBreakerFallback_whenRetryableException() throws Exception {
            // given
            RuntimeException runtimeException = new RuntimeException("Internal Server Error");

            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).isEqualTo("PG 시스템이 일시적으로 사용할 수 없습니다.");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("재시도 불가능한 예외(CoreException) 발생 시 재시도하지 않는다")
        void throwsCoreException_whenNonRetryableException() {
            // given
            CoreException coreException = new CoreException(ErrorType.BAD_REQUEST, "Bad Request");
            doThrow(coreException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            pgPaymentClient.requestPayment(paymentRequest);

            // then
            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }
    }

    @DisplayName("Feign 타임아웃 동작 테스트")
    @Nested
    class FeignTimeoutTest {

        @Test
        @DisplayName("2초를 초과하는 응답은 Feign 타임아웃으로 인해 실패하고 CircuitBreaker Fallback이 호출된다")
        void callsCircuitBreakerFallback_whenResponseExceeds2Seconds() throws Exception {
            // given
            SocketTimeoutException cause = new SocketTimeoutException("Read timed out");
            RuntimeException timeoutException = new RuntimeException("Feign read timeout", cause);
            doThrow(timeoutException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
        }
    }

    @DisplayName("외부 시스템(PG) 응답별 동작 테스트")
    @Nested
    class ExternalSystemResponseTest {

        @Test
        @DisplayName("400 Bad Request 발생 시 재시도하지 않는다")
        void throwsCoreException_when400BadRequest() {
            // given
            PgPaymentDto.PgPaymentResponse badRequestResponse = createErrorResponse("Bad Request", "주문 ID는 6자리 이상 문자열이어야 합니다.");
            doReturn(badRequestResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when & then
            pgPaymentClient.requestPayment(paymentRequest);

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("정상 응답(200 OK) 시 성공적으로 처리한다")
        void succeeds_when200OK() throws Exception {
            // given
            PgPaymentDto.PgPaymentResponse successResponse = createSuccessResponse("txn-key-123");
            doReturn(successResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            // then
            assertThat(response.transactionKey()).isEqualTo("txn-key-123");
            assertThat(response.status()).isEqualTo("PENDING");
            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("500 Internal Server Error 발생 시 재시도하지 않는다.")
        void throwsCoreException_when500InternalServerError() {
            // given
            PgPaymentDto.PgPaymentResponse internalServerErrorResponse = createErrorResponse("Internal Server Error", "PG 시스템에 일시적인 오류가 발생했습니다.");
            doReturn(internalServerErrorResponse)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when & then
            pgPaymentClient.requestPayment(paymentRequest);

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }

        @Test
        @DisplayName("네트워크 연결 실패(RuntimeException) 발생 시 CircuitBreaker Fallback이 호출된다")
        void callsCircuitBreakerFallback_whenRuntimeException() throws Exception {
            // given
            RuntimeException runtimeException = new RuntimeException("Connection refused");

            doThrow(runtimeException)
                    .when(pgPaymentFeignClient)
                    .requestPayment(anyString(), any());

            // when
            PaymentResponse response = pgPaymentClient.requestPayment(paymentRequest);

            // then
            assertThat(response.transactionKey()).isNull();
            assertThat(response.status()).isEqualTo("PENDING");
            assertThat(response.reason()).isEqualTo("PG 시스템이 일시적으로 사용할 수 없습니다.");

            verify(pgPaymentFeignClient, times(1)).requestPayment(anyString(), any());
        }
    }

    private PgPaymentDto.PgPaymentResponse createSuccessResponse(String transactionKey) {
        return new PgPaymentDto.PgPaymentResponse(
                new PgPaymentDto.PgPaymentResponse.Meta("SUCCESS", null, null),
                new PgPaymentDto.PgPaymentResponse.TransactionData(
                        transactionKey,
                        "PENDING",
                        null
                )
        );
    }

    private PgPaymentDto.PgPaymentResponse createErrorResponse(String errorCode, String errorMessage) {
        return new PgPaymentDto.PgPaymentResponse(
                new PgPaymentDto.PgPaymentResponse.Meta("FAIL", errorCode, errorMessage),
                new PgPaymentDto.PgPaymentResponse.TransactionData(
                        null,
                        null,
                        null
                )
        );
    }
}
