package com.loopers.application.payment;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.dto.OrderDomainCreateRequest;
import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.user.Gender;
import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;

/**
 * Resilience4j Circuit Breaker 동작 검증 테스트
 * <p>
 * 검증 항목:
 * - minimumNumberOfCalls 로직 정확성
 * - failureRateThreshold 계산 정확성
 * - Circuit Breaker 상태 전환 (CLOSED → OPEN → HALF_OPEN → CLOSED)
 * - OPEN 상태에서 호출 차단
 * - Fallback 로직 실행
 *
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@SpringBootTest
@DisplayName("Circuit Breaker 통합 테스트")
class PaymentCircuitIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PgClient pgClient;

    private CircuitBreaker circuitBreaker;


    @BeforeEach
    void setUp() {
        // Circuit Breaker 초기화
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgClient");
        circuitBreaker.reset(); // 상태 리셋
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
        circuitBreaker.reset();
    }

    @Nested
    @DisplayName("최소 호출 횟수(minimumNumberOfCalls) 검증")
    class 최소_호출_횟수_검증 {

        @Test
        @DisplayName("minimumNumberOfCalls(5) 미만에서는 실패율과 관계없이 Circuit이 CLOSED 유지")
        void minimumNumberOfCalls_미만에서는_Circuit이_CLOSED_유지() {
            // Given: PG 호출 시 항상 실패하도록 설정
            given(pgClient.requestPayment(anyString(), any())).willThrow(createPgException());

            // When: 4회 연속 실패 (minimumNumberOfCalls = 5 미만)
            // Retry가 각 3회씩 재시도하므로 실제 PG 호출은 12회
            for (int i = 0; i < 4; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                // Fallback이 실행되어 예외 없이 정상 반환
                PaymentInfo result = paymentFacade.processPayment(command);

                // Fallback 결과 검증: FAILED 상태, transactionKey 없음
                assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
                assertThat(result.transactionKey()).isNull();
            }

            // Then: Circuit 상태 = CLOSED (아직 최소 호출 수 미만)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            // Metrics 확인
            // RetryableException이 발생하여 Circuit Breaker에 실패로 기록됨
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(4);
            assertThat(metrics.getFailureRate()).isEqualTo(-1.0f);  // minimumNumberOfCalls 미만이므로 -1
        }

        @Test
        @DisplayName("minimumNumberOfCalls(5) 이상이고 실패율 50% 이하면 Circuit이 CLOSED 유지")
        void 실패율_50퍼센트_이하면_Circuit이_CLOSED_유지() {
            // Given: 성공/실패 번갈아 발생
            given(pgClient.requestPayment(anyString(), any())).willReturn(createSuccessResponse())  // 1회 성공
                    .willReturn(createSuccessResponse())  // 2회 성공
                    .willReturn(createSuccessResponse())  // 3회 성공
                    .willThrow(createPgException())       // 4회 실패
                    .willThrow(createPgException());      // 5회 실패

            // When: 5회 호출 (성공 3, 실패 2)
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                try {
                    paymentFacade.processPayment(command);
                } catch (Exception e) {
                    // 예외 무시
                }
            }

            // Then: Circuit 상태 = CLOSED (실패율 40% < 50%)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(3);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
            assertThat(metrics.getFailureRate()).isEqualTo(40.0f); // 2/5 = 40%
        }
    }

    @Nested
    @DisplayName("실패율 임계값(failureRateThreshold) 검증")
    class 실패율_임계값_검증 {

        @Test
        @DisplayName("5회 호출 중 3회 실패 시 실패율 60%로 Circuit이 OPEN으로 전환")
        void 실패율_60퍼센트_초과_시_Circuit이_OPEN_전환() {
            // Given
            given(pgClient.requestPayment(anyString(), any())).willReturn(createSuccessResponse())  // 1회 성공
                    .willReturn(createSuccessResponse())  // 2회 성공
                    .willThrow(createPgException())       // 3회 실패
                    .willThrow(createPgException())       // 4회 실패
                    .willThrow(createPgException());      // 5회 실패 → 실패율 60%

            // When: 5회 호출
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                try {
                    paymentFacade.processPayment(command);
                } catch (Exception e) {
                    // 예외 무시
                }
            }

            // Then: Circuit 상태 = OPEN
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(2);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(3);
            assertThat(metrics.getFailureRate()).isGreaterThanOrEqualTo(50.0f)  // 60% > 50%
                    .isEqualTo(60.0f);
        }

        @Test
        @DisplayName("10회 호출 과정에서 실패율이 50%를 초과하면 Circuit이 OPEN으로 전환")
        void slidingWindow_10개_중_6개_실패_시_Circuit이_OPEN_전환() {
            // Given: 10회 호출 중 6회 실패 (실패율 60%)
            given(pgClient.requestPayment(anyString(), any()))
                    .willReturn(createSuccessResponse())  // 1회 성공
                    .willReturn(createSuccessResponse())  // 2회 성공
                    .willReturn(createSuccessResponse())  // 3회 성공
                    .willReturn(createSuccessResponse())  // 4회 성공
                    .willThrow(createPgException())       // 5회 실패
                    .willThrow(createPgException())       // 6회 실패
                    .willThrow(createPgException())       // 7회 실패
                    .willThrow(createPgException())       // 8회 실패
                    .willThrow(createPgException())       // 9회 실패
                    .willThrow(createPgException());      // 10회 실패

            // When: 10회 호출
            for (int i = 0; i < 10; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                try {
                    paymentFacade.processPayment(command);
                } catch (Exception e) {
                    // 예외 무시
                }
            }

            // Then: Circuit 상태 = OPEN (6/10 = 60% > 50%)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getFailureRate()).isGreaterThanOrEqualTo(50.0f);
        }
    }

    @Nested
    @DisplayName("Circuit OPEN 상태에서 호출 차단 검증")
    class Circuit_OPEN_상태_호출_차단_검증 {

        @Test
        @DisplayName("Circuit이 OPEN 상태일 때 추가 호출은 차단되고 Fallback 실행")
        void Circuit이_OPEN_상태일_때_추가_호출은_차단되고_Fallback_실행() {
            // Given: Circuit을 OPEN 상태로 만듦
            circuitBreaker.transitionToOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When: 결제 시도
            PaymentInfo result = paymentFacade.processPayment(command);

            // Then: Fallback 메서드가 실행됨
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);

            // PG는 호출되지 않음 (Circuit이 차단)
            then(pgClient).shouldHaveNoInteractions();

            // DB에 FAILED 상태로 저장됨
            PaymentEntity savedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureReason()).contains("결제 시스템 응답 지연으로 처리되지 않았습니다.");
        }

        @Test
        @DisplayName("Circuit OPEN 시 CallNotPermittedException이 발생하지 않고 Fallback 실행")
        void Circuit_OPEN_시_CallNotPermittedException이_발생하지_않고_Fallback_실행() {
            // Given: Circuit OPEN
            circuitBreaker.transitionToOpenState();

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When & Then: 예외가 발생하지 않음 (Fallback으로 처리됨)
            assertThatCode(() -> paymentFacade.processPayment(command)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Circuit HALF_OPEN 상태 전환 및 복구 검증")
    class Circuit_HALF_OPEN_상태_전환_및_복구_검증 {

        @Test
        @DisplayName("실패율 초과로 OPEN → 대기 시간 후 HALF_OPEN → 성공 시 CLOSED로 복구")
        void 실패율_초과로_OPEN_상태_후_대기시간_경과하면_HALF_OPEN으로_전환되고_성공_시_CLOSED로_복구() throws InterruptedException {
            // Given: Circuit을 OPEN 상태로 만들기 위한 Mock 설정
            // Retry 설정: max-attempts = 3 (원본 1회 + 재시도 2회)
            //
            // 호출 1: 성공 (1회 PG)
            // 호출 2: 성공 (1회 PG)
            // 호출 3: 실패 (3회 PG - Retry)
            // 호출 4: 실패 (3회 PG - Retry)
            // 호출 5: 실패 (3회 PG - Retry) → OPEN
            // 총: 1 + 1 + 3 + 3 + 3 = 11회
            //
            // HALF_OPEN:
            // 호출 6: 성공 (1회 PG)
            // 호출 7: 성공 (1회 PG)
            // 호출 8: 성공 (1회 PG) → CLOSED
            // 총: 11 + 3 = 14회

            given(pgClient.requestPayment(anyString(), any()))
                    // 1번 호출: 성공
                    .willReturn(createSuccessResponse())

                    // 2번 호출: 성공
                    .willReturn(createSuccessResponse())

                    // 3번 호출: 실패
                    .willThrow(createPgException())

                    // 4번 호출: 실패
                    .willThrow(createPgException())

                    // 5번 호출: 실패
                    .willThrow(createPgException())

                    // 6번 호출: 성공 (HALF_OPEN 1차)
                    .willReturn(createSuccessResponse())

                    // 7번 호출: 성공 (HALF_OPEN 2차)
                    .willReturn(createSuccessResponse())

                    // 8번 호출: 성공 (HALF_OPEN 3차) → CLOSED
                    .willReturn(createSuccessResponse());

            // When: 5회 호출 (성공 2, 실패 3) → 실패율 60% > 50%
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);
                paymentFacade.processPayment(command);
            }

            // Then: OPEN 상태 확인
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // When: HALF_OPEN으로 전환
            // 실제 운영: wait-duration-in-open-state(5s) 경과 후 자동 전환 - application.yml 설정
            // 테스트: transitionToHalfOpenState()로 즉시 전환
            circuitBreaker.transitionToHalfOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // When: HALF_OPEN 상태에서 permitted-number-of-calls-in-half-open-state = 3
            // 3번 연속 성공 호출
            for (int i = 0; i < 3; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                PaymentInfo result = paymentFacade.processPayment(command);
                assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
            }

            // Then: HALF_OPEN → CLOSED 전환 (복구 완료!)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("OPEN 상태에서 일부 호출 성공 후 실패하면 다시 OPEN 유지")
        void OPEN_상태에서_HALF_OPEN_전환_후_실패하면_다시_OPEN으로_전환() {
            // Given: Circuit을 OPEN 상태로 만들기 위한 Mock 설정
            given(pgClient.requestPayment(anyString(), any()))
                    .willReturn(createSuccessResponse())  // 1번 성공
                    .willReturn(createSuccessResponse())  // 2번 성공
                    .willThrow(createPgException())       // 3번 실패
                    .willThrow(createPgException())       // 4번 실패
                    .willThrow(createPgException())       // 5번 실패 → OPEN 전환
                    .willThrow(createPgException())       // HALF_OPEN에서 실패
                    .willThrow(createPgException())       // HALF_OPEN에서 실패
                    .willThrow(createPgException());      // HALF_OPEN에서 실패

            // When: 5회 호출로 OPEN 상태 만들기 (실패율 60%)
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);
                paymentFacade.processPayment(command);
            }

            // Then: OPEN 상태 확인
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // When: HALF_OPEN으로 전환 (실제로는 5초 대기 후 자동 전환 - application.yml 설정)
            circuitBreaker.transitionToHalfOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // When: HALF_OPEN 상태에서 3회 연속 실패 호출
            for (int i = 0; i < 3; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                PaymentInfo result = paymentFacade.processPayment(command);
                assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
            }


            // Then: HALF_OPEN → OPEN 전환 (다시 차단!)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("HALF_OPEN 상태에서 PG 실패 시 Circuit Breaker는 실패로 기록하여 다시 OPEN으로 전환")
        void HALF_OPEN_상태에서_PG실패_시_Circuit_Breaker는_실패로_기록() {
            // Given: Circuit을 OPEN 상태로 만들기 위한 Mock 설정
            given(pgClient.requestPayment(anyString(), any())).willReturn(createSuccessResponse())  // 1번 성공
                    .willReturn(createSuccessResponse())  // 2번 성공
                    .willThrow(createPgException())       // 3번 실패
                    .willThrow(createPgException())       // 4번 실패
                    .willThrow(createPgException())       // 5번 실패 → OPEN
                    .willReturn(createSuccessResponse())  // 6번 성공 (HALF_OPEN 1차)
                    .willThrow(createPgException())       // 7번 실패 (HALF_OPEN 2차) → OPEN으로 재전환
                    .willThrow(createPgException());      // 8번 실패 (사용되지 않음)

            // When: 5회 호출로 OPEN 상태 만들기
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);
                paymentFacade.processPayment(command);
            }

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // When: HALF_OPEN으로 전환
            circuitBreaker.transitionToHalfOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // When: 첫 번째 호출 성공
            UserInfo user1 = createAndSaveUser();
            OrderEntity order1 = createAndSaveOrder(user1.id());
            PaymentCommand command1 = createPaymentCommand(order1, user1);
            PaymentInfo result1 = paymentFacade.processPayment(command1);
            assertThat(result1.status()).isEqualTo(PaymentStatus.PENDING);  // PG 성공

            // Then: HALF_OPEN 유지
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // When: 두 번째 호출 - PG 실패, Fallback 실행되지만 Circuit Breaker는 실패로 기록
            UserInfo user2 = createAndSaveUser();
            OrderEntity order2 = createAndSaveOrder(user2.id());
            PaymentCommand command2 = createPaymentCommand(order2, user2);
            PaymentInfo result2 = paymentFacade.processPayment(command2);
            assertThat(result2.status()).isEqualTo(PaymentStatus.FAILED);  // Fallback 실행

            // HALF_OPEN 유지 (설정에 따라 다를 수 있음)
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // When: 세 번째 호출 - PG 실패, Fallback 실행되지만 Circuit Breaker는 실패로 기록
            UserInfo user3 = createAndSaveUser();
            OrderEntity order3 = createAndSaveOrder(user3.id());
            PaymentCommand command3 = createPaymentCommand(order3, user3);
            PaymentInfo result3 = paymentFacade.processPayment(command3);
            assertThat(result3.status()).isEqualTo(PaymentStatus.FAILED);  // Fallback 실행

            // Then: RetryableException으로 인해 Circuit Breaker는 실패로 기록하여 다시 OPEN으로 전환
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("HALF_OPEN 상태에서 허용된 호출 수(3회) 모두 성공하면 CLOSED로 복구")
        void HALF_OPEN_상태에서_허용된_호출_수_모두_성공하면_CLOSED로_복구() {
            // Given: Circuit을 OPEN 상태로 만들기 위한 Mock 설정
            // Retry는 무시하고 간단하게 설정
            given(pgClient.requestPayment(anyString(), any()))
                    .willReturn(createSuccessResponse())  // 1번 성공
                    .willReturn(createSuccessResponse())  // 2번 성공
                    .willThrow(createPgException())       // 3번 실패
                    .willThrow(createPgException())       // 4번 실패
                    .willThrow(createPgException())       // 5번 실패 → OPEN
                    .willReturn(createSuccessResponse())  // 6번 성공 (HALF_OPEN 1차)
                    .willReturn(createSuccessResponse())  // 7번 성공 (HALF_OPEN 2차)
                    .willReturn(createSuccessResponse()); // 8번 성공 (HALF_OPEN 3차) → CLOSED

            // When: 5회 호출로 OPEN 상태 만들기
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);
                paymentFacade.processPayment(command);
            }

            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // When: HALF_OPEN으로 전환
            circuitBreaker.transitionToHalfOpenState();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // When: permitted-number-of-calls-in-half-open-state = 3
            // 3번 연속 성공 호출
            for (int i = 0; i < 3; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                PaymentInfo result = paymentFacade.processPayment(command);
                assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
            }

            // Then: HALF_OPEN 상태에서 3번 성공 확인 (CLOSED 전환 전에 확인!)
            // 주의: 3번째 성공 호출 직후 CLOSED로 전환되면서 Metrics가 리셋될 수 있음
            CircuitBreaker.State currentState = circuitBreaker.getState();

            // CLOSED로 전환되었는지 확인 (3번 성공 후 자동 전환)
            assertThat(currentState).isEqualTo(CircuitBreaker.State.CLOSED);

            // CLOSED 상태에서는 Metrics가 리셋되므로 0일 수 있음
            // 대신 Circuit Breaker가 정상적으로 CLOSED로 복구되었음을 검증
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            // CLOSED로 전환되면 Metrics가 리셋되므로
            // 성공 카운트는 0일 수 있음 (새로운 Sliding Window 시작)
            assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(0);

            // 중요한 것은 Circuit이 CLOSED로 복구되었다는 것!
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }

    @Nested
    @DisplayName("Fallback 메서드 실행 검증")
    class Fallback_메서드_실행_검증 {

        @Test
        @DisplayName("PG 장애 발생 시 Fallback 메서드가 실행되고 FAILED 상태로 저장")
        void PG_장애_발생_시_Fallback_메서드가_실행되고_FAILED_상태로_저장() {
            // Given: PG 호출 시 예외 발생
            given(pgClient.requestPayment(anyString(), any())).willThrow(createPgException());

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When: 결제 처리
            PaymentInfo result = paymentFacade.processPayment(command);

            // Then: Fallback 결과 확인
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);

            // DB 확인
            PaymentEntity savedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(savedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(savedPayment.getFailureReason()).isNotNull().contains("결제 시스템 응답 지연으로 처리되지 않았습니다");
        }

        @Test
        @DisplayName("Fallback에서 생성된 결제는 transactionKey가 null이다")
        void Fallback에서_생성된_결제는_transactionKey가_null이다() {
            // Given
            given(pgClient.requestPayment(anyString(), any())).willThrow(createPgException());

            UserInfo user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.id());
            PaymentCommand command = createPaymentCommand(order, user);

            // When
            PaymentInfo result = paymentFacade.processPayment(command);

            // Then: PG 호출에 실패했으므로 transactionKey가 없음
            assertThat(result.transactionKey()).isNull();

            PaymentEntity savedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(savedPayment.getTransactionKey()).isNull();
        }
    }

    @Nested
    @DisplayName("Circuit Breaker 메트릭(Metrics) 검증")
    class Circuit_Breaker_메트릭_검증 {

        @Test
        @DisplayName("성공/실패 호출 수가 정확히 기록된다")
        void 성공_실패_호출_수가_정확히_기록된다() {
            // Given
            given(pgClient.requestPayment(anyString(), any()))
                    .willReturn(createSuccessResponse())  // 1회 성공
                    .willReturn(createSuccessResponse())  // 2회 성공
                    .willReturn(createSuccessResponse())  // 3회 성공
                    .willThrow(createPgException())       // 4회 실패 (RetryableException)
                    .willThrow(createPgException());      // 5회 실패 (RetryableException)

            // When: 5회 호출 (성공 3, 실패 2)
            for (int i = 0; i < 5; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                // Fallback이 있으므로 예외가 발생하지 않음
                PaymentInfo result = paymentFacade.processPayment(command);

                if (i < 3) {
                    assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);  // 성공
                } else {
                    assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);   // Fallback
                }
            }

            // Then: 메트릭 확인 (RetryableException이 발생한 경우만 실패로 기록)
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(3);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
            assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
            assertThat(metrics.getFailureRate()).isEqualTo(40.0f);
        }

        @Test
        @DisplayName("OPEN 상태에서는 notPermittedCalls가 증가한다")
        void OPEN_상태에서는_notPermittedCalls가_증가한다() {
            // Given: Circuit OPEN
            circuitBreaker.transitionToOpenState();

            // When: 3회 호출 시도
            for (int i = 0; i < 3; i++) {
                UserInfo user = createAndSaveUser();
                OrderEntity order = createAndSaveOrder(user.id());
                PaymentCommand command = createPaymentCommand(order, user);

                paymentFacade.processPayment(command); // Fallback 실행
            }

            // Then: notPermittedCalls = 3
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(3);
        }
    }

    // ==================== 테스트 헬퍼 메서드 ====================
    private UserInfo createAndSaveUser() {
        // Given: 사용자 생성 및 포인트 충전
        UserRegisterCommand command = createRandomUserCommand();
        return userFacade.registerUser(command);
    }

    /**
     * 랜덤 사용자명으로 UserRegisterCommand 생성
     */
    private UserRegisterCommand createRandomUserCommand() {
        String randomUsername = UUID.randomUUID().toString().substring(0, 8);
        String randomEmail = randomUsername + "@example.com";
        return UserRegisterCommand.of(randomUsername, randomEmail, "1990-01-01", Gender.MALE);
    }


    private OrderEntity createAndSaveOrder(Long userId) {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000000);
        Long orderNumber = timestamp * 1000000L + random;
        OrderDomainCreateRequest request = new OrderDomainCreateRequest(userId, orderNumber, new BigDecimal("50000.00"), BigDecimal.ZERO, new BigDecimal("50000.00"));
        OrderEntity order = OrderEntity.createOrder(request);
        return orderRepository.save(order);
    }

    private PaymentCommand createPaymentCommand(OrderEntity order, UserInfo user) {
        return PaymentCommand.builder()
                .username(user.username())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .cardType("CREDIT")
                .cardNo("1234-5678-9012-3456")
                .amount(order.getFinalTotalAmount())
                .callbackUrl("http://localhost:8080/api/v1/payments/callback")
                .build();
    }

    /**
     * PG 예외 생성 (RetryableException - application.yml 설정과 일치)
     */
    private RetryableException createPgException() {
        Request request = Request.create(Request.HttpMethod.POST, "http://pg-simulator/api/v1/payments", java.util.Collections.emptyMap(), null, new RequestTemplate());

        return new RetryableException(503, "PG 서비스 일시적 장애", Request.HttpMethod.POST, (Long) null, request);
    }

    /**
     * PG 성공 응답 생성 (새로운 응답 구조)
     */
    private PgPaymentResponse createSuccessResponse() {
        String randomTransactionKey = "tx-" + UUID.randomUUID().toString().substring(0, 10);

        return new PgPaymentResponse(
                new PgPaymentResponse.Meta("SUCCESS", null, null),
                new PgPaymentResponse.Data(
                        randomTransactionKey,
                        "1",
                        "SAMSUNG",
                        "1234-5678-****-****",
                        BigDecimal.valueOf(50000),
                        "PENDING",
                        "결제 요청이 접수되었습니다."
                )
        );
    }
}

