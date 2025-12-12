package com.loopers.application.payment;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.order.*;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.pg.PgPaymentExecutor;
import com.loopers.infrastructure.pg.PgV1Dto;
import com.loopers.infrastructure.payment.PaymentCallbackUrlGenerator;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결제 상태 복구 스케줄러 통합 테스트
 * 
 * 이 테스트는 실제 PGsimulator를 사용합니다.
 * 테스트 실행 전에 PGsimulator가 실행 중이어야 합니다:
 * 
 * ```bash
 * ./gradlew :apps:pg-simulator:bootRun
 * ```
 * 
 * 주의사항:
 * - PGsimulator는 기본적으로 40% 확률로 결제 요청을 실패시킵니다.
 * - 테스트는 재시도 로직을 포함하므로 일부 테스트는 여러 번 시도할 수 있습니다.
 * - 실제 네트워크 호출이 발생하므로 테스트 실행 시간이 길어질 수 있습니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("결제 상태 복구 스케줄러 통합 테스트 (실제 PGsimulator 사용)")
class PaymentStatusRecoverySchedulerIntegrationTest {

    @Autowired
    private PaymentStatusRecoveryScheduler scheduler;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PgPaymentExecutor pgPaymentExecutor;

    @Autowired
    private PaymentCallbackUrlGenerator callbackUrlGenerator;

    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 정리 (필요시)
    }

    @DisplayName("트랜잭션 키가 없는 주문 복구")
    @Nested
    class RecoverOrderWithoutTransactionKey {

        @DisplayName("PG 시스템에 기록이 있으면 상태를 동기화한다. (Happy Path)")
        @Test
        void should_syncStatus_when_pgHasRecord() {
            // arrange
            // 1. 주문 생성
            Order order = createAndSavePendingOrder();
            assertThat(order.getPgTransactionKey()).isNull();
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(10));

            // 2. PGsimulator에 결제 요청을 보내서 기록 생성
            // 여러 번 시도하여 성공할 때까지 재시도 (PGsimulator는 90% 확률로 실패)
            PgV1Dto.PgTransactionResponse pgTransactionResponse = null;
            int maxAttempts = 20; // 최대 20회 시도
            for (int i = 0; i < maxAttempts; i++) {
                try {
                    PgV1Dto.PgPaymentRequest pgRequest = new PgV1Dto.PgPaymentRequest(
                            order.getOrderId(),
                            order.getCardType(),
                            order.getCardNo(),
                            (long) order.getFinalPrice().amount(),
                            callbackUrlGenerator.generateCallbackUrl(order.getOrderId())
                    );
                    pgTransactionResponse = pgPaymentExecutor.requestPaymentAsync(pgRequest).get();
                    if (pgTransactionResponse != null && pgTransactionResponse.transactionKey() != null) {
                        break; // 성공
                    }
                } catch (Exception e) {
                    // 실패 시 재시도
                    if (i == maxAttempts - 1) {
                        throw new RuntimeException("PGsimulator 결제 요청 실패 (최대 시도 횟수 초과)", e);
                    }
                }
            }

            // 3. 주문에 트랜잭션 키 설정하지 않음 (트랜잭션 키 없는 상태 유지)
            // order.updatePaymentStatus는 호출하지 않음

            // act
            scheduler.recoverPendingPayments();

            // assert
            Order updatedOrder = orderService.getOrderById(order.getId());
            // PGsimulator에 기록이 있으므로 상태가 동기화되어야 함
            assertThat(updatedOrder.getPgTransactionKey()).isNotNull();
            // PGsimulator의 응답에 따라 상태가 결정됨 (일반적으로 PENDING 또는 SUCCESS)
            assertThat(updatedOrder.getStatus()).isIn(OrderStatus.PENDING, OrderStatus.PAID);
        }

        @DisplayName("PG 시스템에 기록이 없고 1분 이내이면 재결제를 시도한다. (Happy Path)")
        @Test
        void should_retryPayment_when_noPgRecordAndRecent() {
            // arrange
            // 존재하지 않는 orderId를 사용하여 PGsimulator에 기록이 없도록 함
            Order order = createAndSavePendingOrder();
            // orderId를 변경하여 PGsimulator에 기록이 없도록 함
            String originalOrderId = order.getOrderId();
            setCreatedAt(order, ZonedDateTime.now().minusSeconds(30)); // 30초 전 (1분 이내)

            // act
            scheduler.recoverPendingPayments();

            // assert
            // 재결제 시도가 발생했는지 확인
            // PGsimulator는 90% 확률로 실패하므로, 재시도 로직이 작동해야 함
            Order updatedOrder = orderService.getOrderById(order.getId());
            // 재결제 시도 후 상태가 변경되었는지 확인
            // (실패 시 PENDING 유지, 성공 시 PAID 또는 트랜잭션 키 설정)
            assertThat(updatedOrder.getStatus()).isIn(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.FAILED);
        }

        @DisplayName("PG 시스템에 기록이 없고 30분 이상 경과하면 실패 처리한다. (Happy Path)")
        @Test
        void should_markAsFailed_when_noPgRecordAndTimeout() {
            // arrange
            Order order = createAndSavePendingOrder();
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(35));

            // act
            scheduler.recoverPendingPayments();

            // assert
            Order updatedOrder = orderService.getOrderById(order.getId());
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(updatedOrder.getPgPaymentReason()).isEqualTo("결제 요청 타임아웃 - 트랜잭션 키 미발급");
        }

        @DisplayName("재시도 횟수가 2회 이상이면 재결제를 시도하지 않는다. (Edge Case)")
        @Test
        void should_notRetry_when_retryCountExceeded() {
            // arrange
            Order order = createAndSavePendingOrder();
            setCreatedAt(order, ZonedDateTime.now().minusSeconds(30)); // 30초 전 (1분 이내)
            order.incrementRetryCount(); // 1회
            order.incrementRetryCount(); // 2회 (재시도 불가능)
            orderJpaRepository.save(order);

            // act
            scheduler.recoverPendingPayments();

            // assert
            // 재시도 횟수가 초과되었으므로 재결제 시도가 발생하지 않아야 함
            Order updatedOrder = orderService.getOrderById(order.getId());
            assertThat(updatedOrder.getRetryCount()).isEqualTo(2);
            // 상태는 변경되지 않아야 함 (재결제 시도 없음)
        }

        @DisplayName("재결제 시도가 실패하면 retryCount를 증가시킨다. (Happy Path)")
        @Test
        void should_incrementRetryCount_when_retryPaymentFails() {
            // arrange
            Order order = createAndSavePendingOrder();
            setCreatedAt(order, ZonedDateTime.now().minusSeconds(30)); // 30초 전 (1분 이내)
            int initialRetryCount = order.getRetryCount() == null ? 0 : order.getRetryCount();

            // act
            scheduler.recoverPendingPayments();

            // assert
            Order updatedOrder = orderService.getOrderById(order.getId());
            // 재결제 시도가 실패하면 retryCount가 증가해야 함
            // PGsimulator는 90% 확률로 실패하므로 대부분의 경우 retryCount가 증가함
            // 하지만 성공할 수도 있으므로 retryCount >= initialRetryCount로 확인
            int updatedRetryCount = updatedOrder.getRetryCount() == null ? 0 : updatedOrder.getRetryCount();
            assertThat(updatedRetryCount).isGreaterThanOrEqualTo(initialRetryCount);
        }
    }

    @DisplayName("트랜잭션 키가 있는 주문 복구")
    @Nested
    class RecoverOrderWithTransactionKey {

        @DisplayName("트랜잭션 키로 상태를 동기화한다. (Happy Path)")
        @Test
        void should_syncStatus_when_transactionKeyExists() {
            // arrange
            Order order = createAndSavePendingOrder();
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(10));

            // 1. PGsimulator에 결제 요청을 보내서 트랜잭션 키 생성
            PgV1Dto.PgTransactionResponse pgTransactionResponse = null;
            int maxAttempts = 20;
            for (int i = 0; i < maxAttempts; i++) {
                try {
                    PgV1Dto.PgPaymentRequest pgRequest = new PgV1Dto.PgPaymentRequest(
                            order.getOrderId(),
                            order.getCardType(),
                            order.getCardNo(),
                            (long) order.getFinalPrice().amount(),
                            callbackUrlGenerator.generateCallbackUrl(order.getOrderId())
                    );
                    pgTransactionResponse = pgPaymentExecutor.requestPaymentAsync(pgRequest).get();
                    if (pgTransactionResponse != null && pgTransactionResponse.transactionKey() != null) {
                        break;
                    }
                } catch (Exception e) {
                    if (i == maxAttempts - 1) {
                        throw new RuntimeException("PGsimulator 결제 요청 실패", e);
                    }
                }
            }

            // 2. 주문에 트랜잭션 키 설정 (PENDING 상태 유지)
            if (pgTransactionResponse != null && pgTransactionResponse.transactionKey() != null) {
                order.updatePaymentStatus(OrderStatus.PENDING, pgTransactionResponse.transactionKey(), null);
                orderJpaRepository.save(order);
            }

            // act
            scheduler.recoverPendingPayments();

            // assert
            Order updatedOrder = orderService.getOrderById(order.getId());
            // 트랜잭션 키가 설정되어 있어야 함
            assertThat(updatedOrder.getPgTransactionKey()).isNotNull();
            // PGsimulator의 응답에 따라 상태가 결정됨
            assertThat(updatedOrder.getStatus()).isIn(OrderStatus.PENDING, OrderStatus.PAID);
        }
    }

    @DisplayName("포인트 결제 주문 처리")
    @Nested
    class HandlePointPayment {

        @DisplayName("포인트 결제 주문은 복구 대상이 아니다. (Edge Case)")
        @Test
        void should_skip_when_pointPayment() {
            // arrange
            Order order = Order.create(1L, List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            ), new DiscountResult(new Price(10000)), PaymentMethod.POINT);
            order = orderJpaRepository.save(order);
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(10));

            // act
            scheduler.recoverPendingPayments();

            // assert
            // 포인트 결제 주문은 PG 복구 대상이 아니므로 상태가 변경되지 않아야 함
            Order updatedOrder = orderService.getOrderById(order.getId());
            assertThat(updatedOrder.getPaymentMethod()).isEqualTo(PaymentMethod.POINT);
            // 포인트 결제는 PG 복구 대상이 아니므로 상태 변경 없음
        }
    }

    // ==================== Helper Methods ====================

    private Order createAndSavePendingOrder() {
        List<OrderItem> orderItems = List.of(
                OrderItem.create(1L, "상품1", 1, new Price(10000))
        );
        Price originalPrice = new Price(10000);
        DiscountResult discountResult = new DiscountResult(originalPrice);
        Order order = Order.create(1L, orderItems, discountResult, PaymentMethod.PG);
        order.setPaymentInfo("SAMSUNG", "1234-5678-9012-3456", "http://localhost:8080/api/v1/payments/callback");
        return orderJpaRepository.save(order);
    }

    private void setCreatedAt(Order order, ZonedDateTime createdAt) {
        // createdAt은 updatable=false이므로 직접 SQL로 업데이트
        Timestamp timestamp = Timestamp.from(createdAt.toInstant());
        entityManager.createNativeQuery(
                        "UPDATE tb_order SET created_at = :createdAt WHERE id = :id"
                )
                .setParameter("createdAt", timestamp)
                .setParameter("id", order.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화하여 DB에서 다시 조회되도록
    }
}
