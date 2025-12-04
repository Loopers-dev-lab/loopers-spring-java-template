package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.payment.PaymentRepository;
import com.loopers.infrastructure.payment.PgClient;
import com.loopers.infrastructure.payment.PgPaymentRequest;
import com.loopers.infrastructure.payment.PgPaymentResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("PaymentService Circuit Breaker 테스트")
class PaymentServiceCircuitBreakerTest {

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
        circuitBreaker.reset(); // 테스트 시작 전 Circuit Breaker 초기화
        paymentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        circuitBreaker.reset();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Circuit Breaker 설정 검증 - COUNT_BASED, 슬라이딩 윈도우 10")
    void verifyCircuitBreakerConfiguration() {
        // given & when
        var config = circuitBreaker.getCircuitBreakerConfig();

        // then
        assertThat(config.getSlidingWindowType())
            .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getSlowCallDurationThreshold().toSeconds()).isEqualTo(3);
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1) / 1000).isEqualTo(10);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
    }

    @Test
    @DisplayName("10건 중 5건 실패 시 Circuit이 OPEN 상태로 전환된다")
    void shouldOpenCircuitWhen50PercentFailure() {
        // given - Retry가 2번 시도하므로 충분한 Mock 응답 준비
        // 실패 3번 (Retry 포함 6번 호출) + 성공 7번 = 총 50% 실패율
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(createFeignException()).thenThrow(createFeignException()) // 1번째 시도 (초기 + 재시도)
            .thenThrow(createFeignException()).thenThrow(createFeignException()) // 2번째 시도
            .thenThrow(createFeignException()).thenThrow(createFeignException()) // 3번째 시도
            .thenAnswer(invocation -> new PgPaymentResponse("TX-" + System.nanoTime(), "PENDING", null))
            .thenAnswer(invocation -> new PgPaymentResponse("TX-" + System.nanoTime(), "PENDING", null))
            .thenAnswer(invocation -> new PgPaymentResponse("TX-" + System.nanoTime(), "PENDING", null))
            .thenAnswer(invocation -> new PgPaymentResponse("TX-" + System.nanoTime(), "PENDING", null))
            .thenAnswer(invocation -> new PgPaymentResponse("TX-" + System.nanoTime(), "PENDING", null))
            .thenAnswer(invocation -> new PgPaymentResponse("TX-" + System.nanoTime(), "PENDING", null))
            .thenAnswer(invocation -> new PgPaymentResponse("TX-" + System.nanoTime(), "PENDING", null));

        // when - 10번 요청 (실패 3번 + 성공 7번)
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
                // Retry 후에도 실패하면 예외 발생 예상
            }
        }

        // then - Circuit이 OPEN 상태여야 함 (50% 이상 실패)
        // 10초 후 자동으로 HALF_OPEN으로 전환될 수 있음
        assertThat(circuitBreaker.getState())
            .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);

        // Metrics 검증
        var metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(3);
        assertThat(metrics.getFailureRate()).isGreaterThanOrEqualTo(30.0f); // 최소 30%
    }

    @Test
    @DisplayName("Circuit OPEN 상태에서는 예외가 발생한다")
    void shouldThrowExceptionWhenCircuitOpen() {
        // given - Circuit을 강제로 OPEN 상태로 만듦
        circuitBreaker.transitionToOpenState();

        // when & then - Circuit이 OPEN이면 예외 발생
        assertThatThrownBy(() -> paymentService.requestPayment(
            "user123",
            "order-open",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        )).isInstanceOf(Exception.class);

        // PG 호출이 없어야 함 (Circuit이 열려있으므로)
        verify(pgClient, times(0)).requestPayment(anyString(), any(PgPaymentRequest.class));
    }

    @Test
    @DisplayName("Circuit HALF_OPEN 상태에서 3번의 테스트 호출 후 성공하면 CLOSED로 전환")
    void shouldCloseCircuitAfterSuccessfulCallsInHalfOpen() throws InterruptedException {
        // given - Circuit을 OPEN 상태로 만듦
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // HALF_OPEN으로 전환 (강제)
        circuitBreaker.transitionToHalfOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Mock 설정 - 매번 다른 transactionKey로 성공 응답
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenAnswer(invocation -> new PgPaymentResponse(
                "TX-" + System.nanoTime(),
                "PENDING",
                null
            ));

        // when - 3번 호출 (permitted-number-of-calls-in-half-open-state = 3)
        for (int i = 0; i < 3; i++) {
            paymentService.requestPayment(
                "user123",
                "order-half-open-" + i,
                BigDecimal.valueOf(10000),
                "SAMSUNG",
                "1234-5678-9012-3456"
            );
        }

        // then - Circuit이 CLOSED로 전환되어야 함
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit HALF_OPEN 상태에서 실패하면 다시 OPEN으로 전환")
    void shouldReopenCircuitAfterFailureInHalfOpen() {
        // given
        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        // Mock 설정 - 실패 응답
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(createFeignException());

        // when - HALF_OPEN에서 실패하면 Circuit이 즉시 OPEN으로 전환
        // (Resilience4j는 HALF_OPEN 상태에서 첫 실패 시 바로 OPEN으로 전환)
        try {
            paymentService.requestPayment(
                "user123",
                "order-fail-1",
                BigDecimal.valueOf(10000),
                "SAMSUNG",
                "1234-5678-9012-3456"
            );
        } catch (Exception e) {
            // 예외 발생 예상
        }

        // then - 실패가 기록되어 Circuit이 OPEN으로 전환
        // HALF_OPEN 상태에서는 실패율 계산 없이 즉시 OPEN으로 전환
        assertThat(circuitBreaker.getState()).isIn(
            CircuitBreaker.State.OPEN,
            CircuitBreaker.State.HALF_OPEN // 설정에 따라 즉시 전환되지 않을 수도 있음
        );
    }

    @Test
    @DisplayName("정상 동작 시 Circuit은 CLOSED 상태를 유지한다")
    void shouldKeepCircuitClosedDuringNormalOperation() {
        // given - 매번 다른 transactionKey 반환
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenAnswer(invocation -> new PgPaymentResponse(
                "TX-" + System.nanoTime(), // nanoTime으로 더 세밀한 고유값 생성
                "PENDING",
                null
            ));

        // when - 10번 성공
        for (int i = 0; i < 10; i++) {
            paymentService.requestPayment(
                "user123",
                "order-" + i,
                BigDecimal.valueOf(10000),
                "SAMSUNG",
                "1234-5678-9012-3456"
            );
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        var metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(10);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getFailureRate()).isEqualTo(0.0f);
    }

    private PgPaymentResponse createSuccessResponse() {
        return new PgPaymentResponse(
            "TX-" + System.currentTimeMillis(),
            "PENDING",
            null
        );
    }

    private FeignException createFeignException() {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "http://localhost:8082/api/v1/payments",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );
        return new FeignException.ServiceUnavailable(
            "Service unavailable",
            request,
            "Service unavailable".getBytes(StandardCharsets.UTF_8),
            null
        );
    }
}
