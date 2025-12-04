package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.infrastructure.payment.PgClient;
import com.loopers.infrastructure.payment.PgPaymentRequest;
import com.loopers.infrastructure.payment.PgPaymentResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("PaymentService Retry 테스트")
class PaymentServiceRetryTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private PgClient pgClient;

    @Autowired
    private PaymentRepository paymentRepository;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit");
        circuitBreaker.reset();
        paymentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        circuitBreaker.reset();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("RetryableException 발생 시 1회 재시도한다 (총 2회 호출)")
    void shouldRetryOnceOnRetryableException() {
        // given
        Request request = createMockRequest();

        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(new RetryableException(
                -1,
                "Connection timeout",
                Request.HttpMethod.POST,
                new Date(),
                request
            ))
            .thenReturn(createSuccessResponse()); // 2번째 시도에서 성공

        // when
        Payment result = paymentService.requestPayment(
            "user123",
            "order-retry-test",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("order-retry-test");

        // 총 2번 호출되어야 함 (초기 1회 + 재시도 1회)
        verify(pgClient, times(2)).requestPayment(anyString(), any(PgPaymentRequest.class));
    }

    @Test
    @DisplayName("SocketTimeoutException 발생 시 1회 재시도한다")
    void shouldRetryOnSocketTimeoutException() {
        // given
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(createRetryableException()) // RetryableException으로 테스트
            .thenReturn(createSuccessResponse());


        // when
        Payment result = paymentService.requestPayment(
            "user123",
            "order-timeout",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        );

        // then
        assertThat(result).isNotNull();
        verify(pgClient, times(2)).requestPayment(anyString(), any(PgPaymentRequest.class));
    }

    @Test
    @DisplayName("CoreException 발생 시 재시도하지 않는다")
    void shouldNotRetryOnCoreException() {
        // given
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(new CoreException(ErrorType.BAD_REQUEST, "잘못된 요청"));

        // when & then - CoreException은 재시도되지 않고 즉시 예외 발생
        assertThatThrownBy(() -> paymentService.requestPayment(
            "user123",
            "order-core-exception",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        )).isInstanceOf(CoreException.class);

        // 재시도 없이 1번만 호출
        verify(pgClient, times(1)).requestPayment(anyString(), any(PgPaymentRequest.class));
    }

    @Test
    @DisplayName("2회 모두 실패 시 예외가 발생한다")
    void shouldThrowExceptionAfterAllRetriesFail() {
        // given
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(createRetryableException())
            .thenThrow(createRetryableException()); // 2번 모두 실패

        // when & then - 2회 재시도 후 예외 발생
        assertThatThrownBy(() -> paymentService.requestPayment(
            "user123",
            "order-all-fail",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        )).isInstanceOf(Exception.class);

        // 총 2번 호출 (초기 + 1회 재시도)
        verify(pgClient, times(2)).requestPayment(anyString(), any(PgPaymentRequest.class));
    }

    @Test
    @DisplayName("첫 번째 시도 성공 시 재시도하지 않는다")
    void shouldNotRetryOnFirstSuccess() {
        // given
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenReturn(createSuccessResponse());


        // when
        Payment result = paymentService.requestPayment(
            "user123",
            "order-first-success",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTransactionKey()).doesNotStartWith("TEMP-");

        // 1번만 호출
        verify(pgClient, times(1)).requestPayment(anyString(), any(PgPaymentRequest.class));
    }

    @Test
    @DisplayName("Retry와 Circuit Breaker가 함께 동작한다")
    void shouldWorkWithCircuitBreaker() {
        // given - 연속 실패로 Circuit 열기
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(createFeignException());


        // when - 여러 번 호출하여 Circuit Open
        for (int i = 0; i < 10; i++) {
            try {
                paymentService.requestPayment(
                    "user123",
                    "order-" + i,
                    BigDecimal.valueOf(10000),
                    "SAMSUNG",
                    "1234-5678-9012-3456"
                );
            } catch (Exception e) {
                // 예외 무시
            }
        }

        // then - Circuit이 OPEN 상태
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Circuit이 열린 후에는 PG 호출이 없어야 함
        // 현재까지의 호출 횟수 확인 (Retry 포함하여 각 실패마다 2회씩 호출)
        int callCountBefore = mockingDetails(pgClient).getInvocations().size();

        try {
            paymentService.requestPayment(
                "user123",
                "order-after-open",
                BigDecimal.valueOf(10000),
                "SAMSUNG",
                "1234-5678-9012-3456"
            );
        } catch (Exception e) {
            // Circuit OPEN으로 인한 예외 발생 예상
        }

        int callCountAfter = mockingDetails(pgClient).getInvocations().size();

        // Circuit Open 후 추가 PG 호출이 없어야 함 (Circuit이 차단)
        assertThat(callCountAfter).isEqualTo(callCountBefore);
    }

    private PgPaymentResponse createSuccessResponse() {
        return new PgPaymentResponse(
            "TX-" + System.currentTimeMillis(),
            "PENDING",
            null
        );
    }

    private Request createMockRequest() {
        return Request.create(
            Request.HttpMethod.POST,
            "http://localhost:8082/api/v1/payments",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );
    }

    private RetryableException createRetryableException() {
        return new RetryableException(
            -1,
            "Connection timeout",
            Request.HttpMethod.POST,
            new Date(),
            createMockRequest()
        );
    }

    private FeignException createFeignException() {
        Request request = createMockRequest();
        return new FeignException.ServiceUnavailable(
            "Service unavailable",
            request,
            "Service unavailable".getBytes(StandardCharsets.UTF_8),
            null
        );
    }
}
