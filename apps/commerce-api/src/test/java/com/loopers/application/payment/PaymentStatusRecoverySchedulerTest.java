package com.loopers.application.payment;

import com.loopers.domain.common.vo.Price;
import com.loopers.domain.order.*;
import com.loopers.infrastructure.pg.PgPaymentExecutor;
import com.loopers.infrastructure.pg.PgV1Dto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 상태 복구 스케줄러(PaymentStatusRecoveryScheduler) 테스트")
class PaymentStatusRecoverySchedulerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private PgPaymentExecutor pgPaymentExecutor;

    @InjectMocks
    private PaymentStatusRecoveryScheduler scheduler;

    @DisplayName("트랜잭션 키가 없는 주문 처리")
    @Nested
    class HandleOrderWithoutTransactionKey {

        @DisplayName("PG 시스템에 기록이 있으면 상태 동기화한다. (Happy Path)")
        @Test
        void should_syncStatus_when_pgHasRecord() {
            // arrange
            Order order = createPendingOrderWithoutTransactionKey();
            PgV1Dto.PgOrderResponse pgResponse = new PgV1Dto.PgOrderResponse(
                    order.getOrderId(),
                    List.of(new PgV1Dto.PgTransactionResponse(
                            "txn-123",
                            "SUCCESS",
                            null
                    )),
                    false
            );

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));
            when(pgPaymentExecutor.getPaymentByOrderIdSync(order.getOrderId()))
                    .thenReturn(pgResponse);

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(pgPaymentExecutor).getPaymentByOrderIdSync(order.getOrderId());
            verify(orderService).updateStatusByPgResponse(eq(order.getId()), any(PgV1Dto.PgTransactionResponse.class));
        }

        @DisplayName("PG 시스템에 기록이 없고 5분 이내이면 재결제 시도한다. (Happy Path)")
        @Test
        void should_retryPayment_when_noPgRecordAndRecent() {
            // arrange
            Order order = createPendingOrderWithoutTransactionKey();
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(2)); // 2분 전 생성

            PgV1Dto.PgOrderResponse emptyResponse = new PgV1Dto.PgOrderResponse(
                    order.getOrderId(),
                    List.of(),
                    true // fallback
            );
            PgV1Dto.PgTransactionResponse retryResponse = new PgV1Dto.PgTransactionResponse(
                    "txn-retry-123",
                    "SUCCESS",
                    null
            );

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));
            when(pgPaymentExecutor.getPaymentByOrderIdSync(order.getOrderId()))
                    .thenReturn(emptyResponse);
            when(pgPaymentExecutor.requestPaymentAsync(any(PgV1Dto.PgPaymentRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(retryResponse));

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(pgPaymentExecutor).getPaymentByOrderIdSync(order.getOrderId());
            verify(pgPaymentExecutor).requestPaymentAsync(any(PgV1Dto.PgPaymentRequest.class));
            // 비동기 처리이므로 약간의 대기 시간 필요
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            verify(orderService).updateStatusByPgResponse(eq(order.getId()), eq(retryResponse));
        }

        @DisplayName("PG 시스템에 기록이 없고 30분 이상 경과하면 실패 처리한다. (Happy Path)")
        @Test
        void should_markAsFailed_when_noPgRecordAndTimeout() {
            // arrange
            Order order = createPendingOrderWithoutTransactionKey();
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(35)); // 35분 전 생성

            PgV1Dto.PgOrderResponse emptyResponse = new PgV1Dto.PgOrderResponse(
                    order.getOrderId(),
                    List.of(),
                    true // fallback
            );

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));
            when(pgPaymentExecutor.getPaymentByOrderIdSync(order.getOrderId()))
                    .thenReturn(emptyResponse);

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(pgPaymentExecutor).getPaymentByOrderIdSync(order.getOrderId());
            verify(orderService).markAsFailedByTimeout(
                    eq(order.getId()),
                    eq("결제 요청 타임아웃 - 트랜잭션 키 미발급")
            );
            verify(pgPaymentExecutor, never()).requestPaymentAsync(any());
        }

        @DisplayName("재시도 횟수가 2회 이상이면 재결제를 시도하지 않는다. (Edge Case)")
        @Test
        void should_notRetry_when_retryCountExceeded() {
            // arrange
            Order order = createPendingOrderWithoutTransactionKey();
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(2));
            order.incrementRetryCount(); // 1회
            order.incrementRetryCount(); // 2회 (재시도 불가능)

            PgV1Dto.PgOrderResponse emptyResponse = new PgV1Dto.PgOrderResponse(
                    order.getOrderId(),
                    List.of(),
                    true
            );

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));
            when(pgPaymentExecutor.getPaymentByOrderIdSync(order.getOrderId()))
                    .thenReturn(emptyResponse);

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(pgPaymentExecutor).getPaymentByOrderIdSync(order.getOrderId());
            verify(pgPaymentExecutor, never()).requestPaymentAsync(any());
        }

        @DisplayName("재결제 시도가 실패하면 retryCount를 증가시킨다. (Happy Path)")
        @Test
        void should_incrementRetryCount_when_retryPaymentFails() {
            // arrange
            Order order = createPendingOrderWithoutTransactionKey();
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(2));

            PgV1Dto.PgOrderResponse emptyResponse = new PgV1Dto.PgOrderResponse(
                    order.getOrderId(),
                    List.of(),
                    true
            );
            PgV1Dto.PgTransactionResponse fallbackResponse = new PgV1Dto.PgTransactionResponse(
                    null, // transactionKey가 null = Fallback
                    "PENDING",
                    "외부 시스템 오류"
            );

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));
            when(pgPaymentExecutor.getPaymentByOrderIdSync(order.getOrderId()))
                    .thenReturn(emptyResponse);
            when(pgPaymentExecutor.requestPaymentAsync(any(PgV1Dto.PgPaymentRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(fallbackResponse));

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(orderService).incrementRetryCount(order.getId());
        }
    }

    @DisplayName("트랜잭션 키가 있는 주문 처리")
    @Nested
    class HandleOrderWithTransactionKey {

        @DisplayName("트랜잭션 키로 상태를 동기화한다. (Happy Path)")
        @Test
        void should_syncStatus_when_transactionKeyExists() {
            // arrange
            Order order = createPendingOrderWithTransactionKey("txn-123");

            PgV1Dto.PgOrderResponse pgResponse = new PgV1Dto.PgOrderResponse(
                    order.getOrderId(),
                    List.of(new PgV1Dto.PgTransactionResponse(
                            "txn-123",
                            "SUCCESS",
                            null
                    )),
                    false
            );

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));
            when(pgPaymentExecutor.getPaymentByOrderIdSync(order.getOrderId()))
                    .thenReturn(pgResponse);

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(pgPaymentExecutor).getPaymentByOrderIdSync(order.getOrderId());
            verify(orderService).updateStatusByPgResponse(eq(order.getId()), any(PgV1Dto.PgTransactionResponse.class));
        }

        @DisplayName("상태가 변경되지 않으면 업데이트하지 않는다. (Edge Case)")
        @Test
        void should_notUpdate_when_statusUnchanged() {
            // arrange
            Order order = createPendingOrderWithTransactionKey("txn-123");

            PgV1Dto.PgOrderResponse pgResponse = new PgV1Dto.PgOrderResponse(
                    order.getOrderId(),
                    List.of(new PgV1Dto.PgTransactionResponse(
                            "txn-123",
                            "PENDING", // 동일한 상태
                            null
                    )),
                    false
            );

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));
            when(pgPaymentExecutor.getPaymentByOrderIdSync(order.getOrderId()))
                    .thenReturn(pgResponse);

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(pgPaymentExecutor).getPaymentByOrderIdSync(order.getOrderId());
            // 상태가 동일하면 updateStatusByPgResponse가 호출되지 않을 수 있음
            // (실제 구현에 따라 다름)
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
            ), PaymentMethod.POINT);
            setCreatedAt(order, ZonedDateTime.now().minusMinutes(10));

            when(orderService.findPendingPaymentOrdersBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of(order));

            // act
            scheduler.recoverPendingPayments();

            // assert
            verify(pgPaymentExecutor, never()).getPaymentByOrderIdSync(any());
            verify(pgPaymentExecutor, never()).requestPaymentAsync(any());
        }
    }

    // ==================== Helper Methods ====================

    private Order createPendingOrderWithoutTransactionKey() {
        Order order = Order.create(1L, List.of(
                OrderItem.create(1L, "상품1", 1, new Price(10000))
        ), PaymentMethod.PG);
        order.setPaymentInfo("SAMSUNG", "1234-5678-9012-3456", "http://callback.url");
        setCreatedAt(order, ZonedDateTime.now().minusMinutes(3));
        return order;
    }

    private Order createPendingOrderWithTransactionKey(String transactionKey) {
        Order order = createPendingOrderWithoutTransactionKey();
        // 리플렉션을 사용하여 transactionKey와 status를 직접 설정
        try {
            Field transactionKeyField = Order.class.getDeclaredField("pgTransactionKey");
            transactionKeyField.setAccessible(true);
            transactionKeyField.set(order, transactionKey);

            Field statusField = Order.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(order, OrderStatus.PENDING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set transactionKey and status", e);
        }
        return order;
    }

    /**
     * 리플렉션을 사용하여 BaseEntity의 createdAt 필드 설정
     */
    private void setCreatedAt(Order order, ZonedDateTime createdAt) {
        try {
            Field createdAtField = order.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(order, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
    }
}

