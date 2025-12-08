package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.dto.OrderDomainCreateRequest;
import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.PgGateway;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dtos;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

/*
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@SpringBootTest
@DisplayName("결제 콜백 처리 통합 테스트 (PG-Simulator 기준)")
class PaymentCallbackIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;


    @MockitoBean
    private PgGateway pgGateway;


    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("결제 성공 콜백 처리 (SUCCESS)")
    class 결제_성공_콜백_처리 {

        @Test
        @DisplayName("SUCCESS 콜백 수신 시 결제 완료 처리되고 주문이 확정된다")
        void SUCCESS_콜백_수신_시_결제_완료_처리되고_주문이_확정된다() {
            // Given: 사용자, 주문, PENDING 상태 결제 준비
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            // PG-Simulator가 보내는 실제 콜백 구조
            PaymentV1Dtos.PgCallbackRequest successCallback = createSuccessCallback(payment, order);

            // PG Gateway Mock: PG 조회 시 SUCCESS 응답
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgSuccessResponse(payment, order));

            // When: SUCCESS 콜백 처리
            paymentFacade.handlePaymentCallback(successCallback);

            // Then: 결제 상태 확인
            PaymentEntity updatedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(updatedPayment.getCompletedAt()).isNotNull();

            // Then: 비동기 이벤트 처리 대기 (주문 확정)
            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        OrderEntity updatedOrder = orderRepository.findByOrderNumberAndUserId(order.getOrderNumber(), user.getId()).orElseThrow();
                        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                    });
        }

        @Test
        @DisplayName("SUCCESS 콜백 수신 후 이벤트가 정상 발행된다")
        void SUCCESS_콜백_수신_후_이벤트가_정상_발행된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest successCallback = createSuccessCallback(payment, order);

            // PG Gateway Mock
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgSuccessResponse(payment, order));

            // When
            paymentFacade.handlePaymentCallback(successCallback);

            // Then: 결제 완료 확인
            PaymentEntity updatedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

            // Then: 주문 확정 확인 (이벤트 처리 결과)
            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        OrderEntity updatedOrder = orderRepository.findByOrderNumberAndUserId(order.getOrderNumber(), user.getId()).orElseThrow();
                        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                    });
        }
    }

    @Nested
    @DisplayName("결제 실패 콜백 처리 (FAILED)")
    class 결제_실패_콜백_처리 {

        @Test
        @DisplayName("FAILED 콜백 수신 시 결제 실패 처리되고 주문이 취소된다")
        void FAILED_콜백_수신_시_결제_실패_처리되고_주문이_취소된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            String failureReason = "잔액 부족";
            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, failureReason);

            // PG Gateway Mock: PG 조회 시 FAILED 응답
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgFailedResponse(payment, order, failureReason));

            // When
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then: 결제 실패 확인
            PaymentEntity updatedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(updatedPayment.getFailureReason()).isEqualTo(failureReason);

            // Then: 주문 취소 확인
            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        OrderEntity updatedOrder = orderRepository.findByOrderNumberAndUserId(order.getOrderNumber(), user.getId()).orElseThrow();
                        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                    });
        }

        @Test
        @DisplayName("FAILED 콜백에 실패 사유가 포함되어 저장된다")
        void FAILED_콜백에_실패_사유가_포함되어_저장된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            String failureReason = "카드 한도 초과";
            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, failureReason);

            // PG Gateway Mock
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgFailedResponse(payment, order, failureReason));

            // When
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then
            PaymentEntity updatedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(updatedPayment.getFailureReason()).isEqualTo(failureReason);
        }

        @Test
        @DisplayName("FAILED 콜백 수신 후 주문 취소 이벤트가 발행된다")
        void FAILED_콜백_수신_후_주문_취소_이벤트가_발행된다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            String failureReason = "유효하지 않은 카드";
            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, failureReason);

            // PG Gateway Mock
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgFailedResponse(payment, order, failureReason));

            // When
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then: 비동기 이벤트 처리 대기
            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        OrderEntity updatedOrder = orderRepository.findByOrderNumberAndUserId(order.getOrderNumber(), user.getId()).orElseThrow();
                        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                    });
        }
    }

    @Nested
    @DisplayName("멱등성 검증")
    class 멱등성_검증 {

        @Test
        @DisplayName("이미 COMPLETED 상태인 결제에 SUCCESS 콜백이 다시 오면 처리를 무시한다")
        void 이미_COMPLETED_상태인_결제에_SUCCESS_콜백이_다시_오면_처리를_무시한다() {
            // Given: COMPLETED 상태 결제
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            // 첫 번째 콜백으로 COMPLETED 상태로 만듦
            PaymentV1Dtos.PgCallbackRequest firstCallback = createSuccessCallback(payment, order);

            // PG Gateway Mock
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgSuccessResponse(payment, order));

            paymentFacade.handlePaymentCallback(firstCallback);

            // 상태 확인
            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        PaymentEntity completedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
                        assertThat(completedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
                    });

            PaymentEntity completedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            var originalCompletedAt = completedPayment.getCompletedAt();

            // When: 두 번째 SUCCESS 콜백 (중복)
            PaymentV1Dtos.PgCallbackRequest duplicateCallback = createSuccessCallback(payment, order);
            paymentFacade.handlePaymentCallback(duplicateCallback);

            // Then: 상태 변경 없음
            PaymentEntity unchangedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(unchangedPayment.getCompletedAt()).isEqualTo(originalCompletedAt);
        }

        @Test
        @DisplayName("이미 FAILED 상태인 결제에 FAILED 콜백이 다시 오면 처리를 무시한다")
        void 이미_FAILED_상태인_결제에_FAILED_콜백이_다시_오면_처리를_무시한다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            // 첫 번째 실패 콜백
            String firstReason = "첫 번째 실패";
            PaymentV1Dtos.PgCallbackRequest firstCallback = createFailedCallback(payment, order, firstReason);

            // PG Gateway Mock
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgFailedResponse(payment, order, firstReason));

            paymentFacade.handlePaymentCallback(firstCallback);

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        PaymentEntity failedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
                        assertThat(failedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
                    });

            PaymentEntity failedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            String originalFailureReason = failedPayment.getFailureReason();

            // When: 두 번째 실패 콜백 (다른 사유)
            String secondReason = "두 번째 실패";
            PaymentV1Dtos.PgCallbackRequest duplicateCallback = createFailedCallback(payment, order, secondReason);
            paymentFacade.handlePaymentCallback(duplicateCallback);

            // Then: failureReason 변경 없음 (첫 번째 값 유지)
            PaymentEntity unchangedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(unchangedPayment.getFailureReason()).isEqualTo(originalFailureReason);
        }

        @Test
        @DisplayName("COMPLETED 상태에 FAILED 콜백이 오면 처리를 무시한다")
        void COMPLETED_상태에_FAILED_콜백이_오면_처리를_무시한다() {
            // Given: COMPLETED 상태 결제
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest successCallback = createSuccessCallback(payment, order);

            // PG Gateway Mock
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgSuccessResponse(payment, order));

            paymentFacade.handlePaymentCallback(successCallback);

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        PaymentEntity completedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
                        assertThat(completedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
                    });

            // When: FAILED 콜백 시도
            PaymentV1Dtos.PgCallbackRequest failedCallback = createFailedCallback(payment, order, "뒤늦은 실패");
            paymentFacade.handlePaymentCallback(failedCallback);

            // Then: 여전히 COMPLETED 상태 유지
            PaymentEntity unchangedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(unchangedPayment.getFailureReason()).isNull();
        }
    }

    @Nested
    @DisplayName("PENDING 상태 콜백 처리")
    class PENDING_상태_콜백_처리 {

        @Test
        @DisplayName("PENDING 콜백 수신 시 로그만 출력하고 상태 변경하지 않는다")
        void PENDING_콜백_수신_시_로그만_출력하고_상태_변경하지_않는다() {
            // Given
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user.getId());
            PaymentEntity payment = createAndSavePendingPayment(user, order);

            PaymentV1Dtos.PgCallbackRequest pendingCallback = new PaymentV1Dtos.PgCallbackRequest(
                    payment.getTransactionKey(),
                    order.getOrderNumber().toString(),
                    "CREDIT",
                    "1234-****-****-3456",
                    50000L,
                    "PENDING",
                    null
            );

            // PG Gateway Mock: PG 조회 시 PENDING 응답
            given(pgGateway.getPayment(eq(user.getUsername()), eq(payment.getTransactionKey())))
                    .willReturn(createPgPendingResponse(payment, order));

            // When
            paymentFacade.handlePaymentCallback(pendingCallback);

            // Then: PENDING 상태 유지
            PaymentEntity unchangedPayment = paymentRepository.findByOrderNumber(order.getOrderNumber()).orElseThrow();
            assertThat(unchangedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    // ==================== 테스트 헬퍼 메서드 ====================

    private UserEntity createAndSaveUser() {
        UserEntity user = UserTestFixture.createDefaultUserEntity();
        return userRepository.save(user);
    }

    private OrderEntity createAndSaveOrder(Long userId) {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000000);
        Long orderNumber = timestamp * 1000000L + random;

        OrderDomainCreateRequest request = new OrderDomainCreateRequest(
                userId,
                orderNumber,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                new BigDecimal("50000.00")
        );
        OrderEntity order = OrderEntity.createOrder(request);
        return orderRepository.save(order);
    }

    private PaymentEntity createAndSavePendingPayment(UserEntity user, OrderEntity order) {
        PaymentCommand command = PaymentCommand.builder()
                .username(user.getUsername())
                .orderId(order.getOrderNumber())
                .orderNumber(order.getOrderNumber())
                .cardType("CREDIT")
                .cardNo("1234-5678-9012-3456")
                .amount(order.getFinalTotalAmount())
                .callbackUrl("http://localhost:8080/api/v1/payments/callback")
                .build();

        PaymentEntity payment = PaymentEntity.createPending(user, command);
        payment.updateTransactionKey("TXN_TEST_" + System.currentTimeMillis());
        return paymentRepository.save(payment);
    }

    /**
     * PG-Simulator의 SUCCESS 콜백 생성
     */
    private PaymentV1Dtos.PgCallbackRequest createSuccessCallback(PaymentEntity payment, OrderEntity order) {
        return new PaymentV1Dtos.PgCallbackRequest(
                payment.getTransactionKey(),
                order.getOrderNumber().toString(),
                "CREDIT",
                "1234-****-****-3456",
                order.getFinalTotalAmount().longValue(),
                "SUCCESS",
                null
        );
    }

    /**
     * PG-Simulator의 FAILED 콜백 생성
     */
    private PaymentV1Dtos.PgCallbackRequest createFailedCallback(PaymentEntity payment, OrderEntity order, String reason) {
        return new PaymentV1Dtos.PgCallbackRequest(
                payment.getTransactionKey(),
                order.getOrderNumber().toString(),
                "CREDIT",
                "1234-****-****-3456",
                order.getFinalTotalAmount().longValue(),
                "FAILED",
                reason
        );
    }

    /**
     * PG SUCCESS 응답 생성 (Mock용)
     */
    private PgPaymentResponse createPgSuccessResponse(PaymentEntity payment, OrderEntity order) {
        return new PgPaymentResponse(
                new PgPaymentResponse.Meta("SUCCESS", null, null),
                new PgPaymentResponse.Data(
                        payment.getTransactionKey(),
                        order.getOrderNumber().toString(),
                        "CREDIT",
                        "1234-****-****-3456",
                        order.getFinalTotalAmount(),
                        "SUCCESS",
                        "정상 승인되었습니다."
                )
        );
    }

    /**
     * PG FAILED 응답 생성 (Mock용)
     */
    private PgPaymentResponse createPgFailedResponse(PaymentEntity payment, OrderEntity order, String reason) {
        return new PgPaymentResponse(
                new PgPaymentResponse.Meta("SUCCESS", null, null),
                new PgPaymentResponse.Data(
                        payment.getTransactionKey(),
                        order.getOrderNumber().toString(),
                        "CREDIT",
                        "1234-****-****-3456",
                        order.getFinalTotalAmount(),
                        "FAILED",
                        reason
                )
        );
    }

    /**
     * PG PENDING 응답 생성 (Mock용)
     */
    private PgPaymentResponse createPgPendingResponse(PaymentEntity payment, OrderEntity order) {
        return new PgPaymentResponse(
                new PgPaymentResponse.Meta("SUCCESS", null, null),
                new PgPaymentResponse.Data(
                        payment.getTransactionKey(),
                        order.getOrderNumber().toString(),
                        "CREDIT",
                        "1234-****-****-3456",
                        order.getFinalTotalAmount(),
                        "PENDING",
                        null
                )
        );
    }
}

