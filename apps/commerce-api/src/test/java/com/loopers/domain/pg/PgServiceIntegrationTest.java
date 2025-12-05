package com.loopers.domain.pg;

import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@SpringBootTest
class PgServiceIntegrationTest {

    @Autowired
    private PgService pgService;

    @MockitoBean
    private PgClient pgClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        // 테스트 전 CircuitBreaker 초기화
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit");
        circuitBreaker.reset();
    }

    @Nested
    @DisplayName("Resilience4j 설정 검증")
    class Resilience4jConfigurationTest {

        @DisplayName("CircuitBreaker 설정이 올바르게 로드되었다.")
        @Test
        void circuitBreakerConfiguration() {
            // when
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit");

            // then
            assertThat(circuitBreaker).isNotNull();
            assertThat(circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize())
                    .isEqualTo(10);
            assertThat(circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold())
                    .isEqualTo(50.0f);
            assertThat(circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState())
                    .isEqualTo(2);
            assertThat(circuitBreaker.getCircuitBreakerConfig().getSlowCallDurationThreshold())
                    .isEqualTo(Duration.ofSeconds(2));
            assertThat(circuitBreaker.getCircuitBreakerConfig().getSlowCallRateThreshold())
                    .isEqualTo(50.0f);
            assertThat(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls())
                    .isEqualTo(5);
            assertThat(circuitBreaker.getCircuitBreakerConfig().isAutomaticTransitionFromOpenToHalfOpenEnabled())
                    .isTrue();
        }

        @DisplayName("Retry 설정이 올바르게 로드되었다.")
        @Test
        void retryConfiguration() {
            // when
            Retry retry = retryRegistry.retry("pgRetry");

            // then
            assertThat(retry).isNotNull();
            assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
            assertThat(retry.getRetryConfig().getIntervalFunction().apply(1))
                    .isEqualTo(1000L); // 1초
        }

        @DisplayName("Exponential Backoff가 올바르게 설정되었다.")
        @Test
        void exponentialBackoffConfiguration() {
            // when
            Retry retry = retryRegistry.retry("pgRetry");

            // then
            // 1차 재시도: 1초
            assertThat(retry.getRetryConfig().getIntervalFunction().apply(1))
                    .isEqualTo(1000L);

            // 2차 재시도: 2초 (multiplier: 2)
            assertThat(retry.getRetryConfig().getIntervalFunction().apply(2))
                    .isEqualTo(2000L);
        }
    }

    @Nested
    @DisplayName("Retry 동작 검증")
    class RetryBehaviorTest {

        @DisplayName("일시적 오류 발생 시 최대 3회 재시도한다.")
        @Test
        void retryOnTransientFailure() {
            // given
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(new IOException("네트워크 오류")) // 재시도 대상 예외
                    .willThrow(new IOException("네트워크 오류"))
                    .willReturn(new PgPaymentResponse("tx-123", "SUCCESS", "완료"));

            // when
            String transactionId = pgService.requestPayment(
                    "user123", "order456", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            );

            // then
            assertThat(transactionId).isEqualTo("tx-123");
            then(pgClient).should(times(3)).requestPayment(anyString(), any(PgPaymentRequest.class));
        }

        @DisplayName("재시도 가능한 예외: RetryableException")
        @Test
        void retryOnRetryableException() {
            // given
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(mock(RetryableException.class))
                    .willReturn(new PgPaymentResponse("tx-456", "SUCCESS", "완료"));

            // when
            String transactionId = pgService.requestPayment(
                    "user123", "order456", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            );

            // then
            assertThat(transactionId).isEqualTo("tx-456");
            then(pgClient).should(times(2)).requestPayment(anyString(), any(PgPaymentRequest.class));
        }

        @DisplayName("CoreException은 재시도하지 않는다.")
        @Test
        void doNotRetryOnCoreException() {
            // given
            CoreException coreException = new CoreException(ErrorType.BAD_REQUEST, "잘못된 요청");
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(coreException);

            // when & then
            assertThatThrownBy(() -> pgService.requestPayment(
                    "user123", "order456", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            ))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("잘못된 요청");

            // CoreException은 재시도 대상이 아니므로 1회만 호출
            then(pgClient).should(times(1)).requestPayment(anyString(), any(PgPaymentRequest.class));
        }

        @DisplayName("최대 재시도 횟수를 초과하면 Fallback이 실행된다.")
        @Test
        void fallbackAfterMaxRetries() {
            // given
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(new IOException("네트워크 오류"));

            // when & then
            assertThatThrownBy(() -> pgService.requestPayment(
                    "user123", "order456", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            ))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("결제 시스템이 일시적으로 불안정합니다");

            // 최초 1회 + 재시도 2회 = 총 3회
            then(pgClient).should(times(3)).requestPayment(anyString(), any(PgPaymentRequest.class));
        }
    }

    @Nested
    @DisplayName("CircuitBreaker 동작 검증")
    class CircuitBreakerBehaviorTest {

        @DisplayName("실패율 50% 초과 시 CircuitBreaker가 OPEN 상태로 전환된다.")
        @Test
        void circuitBreakerOpensOnHighFailureRate() {
            // given
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit");
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(new RuntimeException("PG 시스템 오류"));

            // when - 10번 호출 (slidingWindowSize) 중 10번 모두 실패 (100% 실패율)
            for (int i = 0; i < 10; i++) {
                try {
                    pgService.requestPayment(
                            "user123", "order" + i, "SAMSUNG", "1234-5678", "10000", "http://callback.url"
                    );
                } catch (Exception ignored) {
                }
            }

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @DisplayName("CircuitBreaker OPEN 상태일 때 Fallback이 즉시 실행된다.")
        @Test
        void fallbackWhenCircuitBreakerOpen() {
            // given
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit");
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(new RuntimeException("PG 시스템 오류"));

            // CircuitBreaker를 OPEN 상태로 만들기
            for (int i = 0; i < 10; i++) {
                try {
                    pgService.requestPayment(
                            "user123", "order" + i, "SAMSUNG", "1234-5678", "10000", "http://callback.url"
                    );
                } catch (Exception ignored) {
                }
            }

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // when & then - Fallback 실행 (PgClient 호출 없이)
            assertThatThrownBy(() -> pgService.requestPayment(
                    "user123", "order999", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            ))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("결제 시스템이 일시적으로 불안정합니다");

            // OPEN 상태에서는 PgClient를 호출하지 않으므로 여전히 10회 (Retry 포함 30회)
            then(pgClient).should(atMost(30)).requestPayment(anyString(), any(PgPaymentRequest.class));
        }

        @DisplayName("느린 응답도 실패로 간주된다 (Slow Call Rate).")
        @Test
        void circuitBreakerOpensOnSlowCalls() throws Exception {
            // given
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit");
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willAnswer(invocation -> {
                        Thread.sleep(2500); // 2.5초 지연 (threshold: 2초)
                        return new PgPaymentResponse("tx-slow", "SUCCESS", "완료");
                    });

            // when - 10번 호출, 모두 느린 응답
            for (int i = 0; i < 10; i++) {
                try {
                    pgService.requestPayment(
                            "user123", "order" + i, "SAMSUNG", "1234-5678", "10000", "http://callback.url"
                    );
                } catch (Exception ignored) {
                }
            }

            // then - Slow Call Rate 50% 초과로 OPEN 상태
            assertThat(circuitBreaker.getState()).isIn(
                    CircuitBreaker.State.OPEN,
                    CircuitBreaker.State.HALF_OPEN // 타이밍에 따라 Half-Open일 수도 있음
            );
        }
    }

    @Nested
    @DisplayName("Fallback 메서드 검증")
    class FallbackMethodTest {

        @DisplayName("Fallback은 CoreException을 발생시킨다.")
        @Test
        void fallbackThrowsCoreException() {
            // given
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(new RuntimeException("PG 장애"));

            // when & then
            assertThatThrownBy(() -> pgService.requestPayment(
                    "user123", "order456", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            ))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.INTERNAL_ERROR);
        }

        @DisplayName("Fallback 메시지가 명확하다.")
        @Test
        void fallbackMessage() {
            // given
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willThrow(new RuntimeException("PG 장애"));

            // when & then
            assertThatThrownBy(() -> pgService.requestPayment(
                    "user123", "order456", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            ))
                    .hasMessageContaining("결제 시스템이 일시적으로 불안정합니다");
        }
    }

    @Nested
    @DisplayName("getPaymentDetail - Resilience 적용 확인")
    class GetPaymentDetailResilienceTest {

        @DisplayName("getPaymentDetail도 Retry가 적용된다.")
        @Test
        void getPaymentDetail_withRetry() {
            // given
            given(pgClient.getPaymentDetail(anyString(), anyString()))
                    .willThrow(new IOException("네트워크 오류"))
                    .willReturn(new PgPaymentResponse("tx-123", "SUCCESS", "완료"));

            // when
            PgPaymentResponse response = pgService.getPaymentDetail("user123", "tx-123");

            // then
            assertThat(response.transactionId()).isEqualTo("tx-123");
            then(pgClient).should(times(2)).getPaymentDetail(anyString(), anyString());
        }

        @DisplayName("getPaymentDetail도 Fallback이 작동한다.")
        @Test
        void getPaymentDetail_withFallback() {
            // given
            given(pgClient.getPaymentDetail(anyString(), anyString()))
                    .willThrow(new RuntimeException("PG 장애"));

            // when
            PgPaymentResponse response = pgService.getPaymentDetail("user123", "tx-123");

            // then - Fallback에서 null 반환
            assertThat(response).isNull();
        }
    }
}
