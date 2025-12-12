package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.client.PgClientService;
import com.loopers.infrastructure.payment.client.dto.PgClientDto;
import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentFacadeIntegrationTest {

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PgClientService pgClientService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("결제를 요청할 때,")
    @Nested
    class RequestPayment {

        @DisplayName("유효한 주문에 대해 결제 요청하면, PENDING 상태의 결제가 생성된다.")
        @Test
        void createsPayment_whenValidOrderIsProvided() {
            // arrange
            String userId = "user123";
            Order order = createOrder(userId, 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);
            Long orderId = order.getId();

            // PG Client Mock 설정
            PgClientDto.TransactionResponse pgResponse = new PgClientDto.TransactionResponse(
                    "20250101:TR:123456",
                    PgClientDto.TransactionStatus.PENDING,
                    null
            );
            when(pgClientService.requestPayment(anyString(), any(PgClientDto.PaymentRequest.class)))
                    .thenReturn(new PgClient.ApiResponse<>(true, pgResponse, null, null));

            PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
                    userId,
                    orderId,
                    CardType.SAMSUNG,
                    "1234-5678-9012-3456",
                    BigDecimal.valueOf(10000),
                    "http://localhost:8080/callback"
            );

            // act
            PaymentInfo result = paymentFacade.requestPayment(command);

            // assert
            assertAll(
                    () -> assertThat(result.getId()).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getOrderId()).isEqualTo(orderId),
                    () -> assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING),
                    () -> assertThat(result.getPgTransactionKey()).isEqualTo("20250101:TR:123456")
            );
        }

        @DisplayName("존재하지 않는 주문에 대해 결제 요청하면, 예외가 발생한다.")
        @Test
        void throwsException_whenOrderDoesNotExist() {
            // arrange
            PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
                    "user123",
                    999L,
                    CardType.SAMSUNG,
                    "1234-5678-9012-3456",
                    BigDecimal.valueOf(10000),
                    "http://localhost:8080/callback"
            );

            // act & assert
            assertThrows(IllegalArgumentException.class, () -> {
                paymentFacade.requestPayment(command);
            });
        }

        @DisplayName("다른 사용자의 주문에 대해 결제 요청하면, 예외가 발생한다.")
        @Test
        void throwsException_whenOrderBelongsToDifferentUser() {
            // arrange
            Order order = createOrder("user123", 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);

            PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
                    "user999",
                    order.getId(),
                    CardType.SAMSUNG,
                    "1234-5678-9012-3456",
                    BigDecimal.valueOf(10000),
                    "http://localhost:8080/callback"
            );

            // act & assert
            assertThrows(IllegalStateException.class, () -> {
                paymentFacade.requestPayment(command);
            });
        }
    }

    @DisplayName("PG 콜백을 처리할 때,")
    @Nested
    class HandleCallback {

        @DisplayName("결제 성공 콜백을 받으면, Payment와 Order 상태가 업데이트된다.")
        @Test
        void updatesPaymentAndOrder_whenSuccessCallbackReceived() {
            // arrange
            String userId = "user123";
            Order order = createOrder(userId, 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);

            Payment payment = Payment.create(
                    userId, order.getId(), CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.assignPgTransactionKey("20250101:TR:123456");
            payment = paymentRepository.save(payment);

            PaymentCommand.UpdatePaymentStatus command = new PaymentCommand.UpdatePaymentStatus(
                    "20250101:TR:123456",
                    "SUCCESS",
                    null
            );

            // act
            paymentFacade.handleCallback(command);

            // assert
            Payment updatedPayment = paymentRepository.findById(payment.getId()).get();
            Order updatedOrder = orderRepository.findById(order.getId()).get();

            assertAll(
                    () -> assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS),
                    () -> assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED)
            );
        }

        @DisplayName("결제 실패 콜백을 받으면, Payment 상태만 업데이트되고 Order는 PENDING 유지된다.")
        @Test
        void updatesPaymentOnly_whenFailedCallbackReceived() {
            // arrange
            String userId = "user123";
            Order order = createOrder(userId, 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);

            Payment payment = Payment.create(
                    userId, order.getId(), CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.assignPgTransactionKey("20250101:TR:123456");
            payment = paymentRepository.save(payment);

            PaymentCommand.UpdatePaymentStatus command = new PaymentCommand.UpdatePaymentStatus(
                    "20250101:TR:123456",
                    "FAILED",
                    "카드 한도 초과"
            );

            // act
            paymentFacade.handleCallback(command);

            // assert
            Payment updatedPayment = paymentRepository.findById(payment.getId()).get();
            Order updatedOrder = orderRepository.findById(order.getId()).get();

            assertAll(
                    () -> assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                    () -> assertThat(updatedPayment.getFailureReason()).isEqualTo("카드 한도 초과"),
                    () -> assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PENDING)
            );
        }
    }

    @DisplayName("결제 상태를 동기화할 때,")
    @Nested
    class SyncPaymentStatus {

        @DisplayName("PG에서 성공 상태를 조회하면, Payment와 Order가 업데이트된다.")
        @Test
        void updatesPaymentAndOrder_whenPgReturnsSuccess() {
            // arrange
            String userId = "user123";
            Order order = createOrder(userId, 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);

            Payment payment = Payment.create(
                    userId, order.getId(), CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.assignPgTransactionKey("20250101:TR:123456");
            payment = paymentRepository.save(payment);

            // PG Client Mock 설정
            PgClientDto.TransactionDetailResponse pgResponse = new PgClientDto.TransactionDetailResponse(
                    "20250101:TR:123456",
                    String.valueOf(order.getId()),
                    PgClientDto.CardType.SAMSUNG,
                    "1234-****-****-3456",
                    10000L,
                    PgClientDto.TransactionStatus.SUCCESS,
                    null
            );
            when(pgClientService.getTransaction(anyString(), anyString()))
                    .thenReturn(new PgClient.ApiResponse<>(true, pgResponse, null, null));

            // act
            paymentFacade.syncPaymentStatus(payment.getId());

            // assert
            Payment updatedPayment = paymentRepository.findById(payment.getId()).get();
            Order updatedOrder = orderRepository.findById(order.getId()).get();

            assertAll(
                    () -> assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS),
                    () -> assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED)
            );
        }

        @DisplayName("PG에서 실패 상태를 조회하면, Payment만 업데이트된다.")
        @Test
        void updatesPaymentOnly_whenPgReturnsFailed() {
            // arrange
            String userId = "user123";
            Order order = createOrder(userId, 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);

            Payment payment = Payment.create(
                    userId, order.getId(), CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.assignPgTransactionKey("20250101:TR:123456");
            payment = paymentRepository.save(payment);

            // PG Client Mock 설정
            PgClientDto.TransactionDetailResponse pgResponse = new PgClientDto.TransactionDetailResponse(
                    "20250101:TR:123456",
                    String.valueOf(order.getId()),
                    PgClientDto.CardType.SAMSUNG,
                    "1234-****-****-3456",
                    10000L,
                    PgClientDto.TransactionStatus.FAILED,
                    "잘못된 카드 번호"
            );
            when(pgClientService.getTransaction(anyString(), anyString()))
                    .thenReturn(new PgClient.ApiResponse<>(true, pgResponse, null, null));

            // act
            paymentFacade.syncPaymentStatus(payment.getId());

            // assert
            Payment updatedPayment = paymentRepository.findById(payment.getId()).get();
            Order updatedOrder = orderRepository.findById(order.getId()).get();

            assertAll(
                    () -> assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                    () -> assertThat(updatedPayment.getFailureReason()).isEqualTo("잘못된 카드 번호"),
                    () -> assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PENDING)
            );
        }
    }

    @DisplayName("결제 상세를 조회할 때,")
    @Nested
    class GetPaymentDetail {

        @DisplayName("본인의 결제 정보를 조회하면, 정상적으로 반환된다.")
        @Test
        void returnsPaymentInfo_whenOwnPaymentIsRequested() {
            // arrange
            String userId = "user123";
            Order order = createOrder(userId, 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);
            Long orderId = order.getId();

            Payment payment = Payment.create(
                    userId, orderId, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment = paymentRepository.save(payment);
            Long paymentId = payment.getId();

            // act
            PaymentInfo result = paymentFacade.getPaymentDetail(paymentId, userId);

            // assert
            assertAll(
                    () -> assertThat(result.getId()).isEqualTo(paymentId),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getOrderId()).isEqualTo(orderId)
            );
        }

        @DisplayName("다른 사용자의 결제 정보를 조회하면, 예외가 발생한다.")
        @Test
        void throwsException_whenOtherUsersPaymentIsRequested() {
            // arrange
            String userId = "user123";
            Order order = createOrder(userId, 1L, BigDecimal.valueOf(10000));
            order = orderRepository.save(order);

            Payment payment = Payment.create(
                    userId, order.getId(), CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment = paymentRepository.save(payment);
            Long paymentId = payment.getId();

            // act & assert
            assertThrows(IllegalStateException.class, () -> {
                paymentFacade.getPaymentDetail(paymentId, "user999");
            });
        }
    }

    // === Helper Methods ===

    private Order createOrder(String userId, Long productId, BigDecimal totalPrice) {
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(OrderItem.create(productId, "테스트 상품", BigDecimal.valueOf(10000), 1));

        return Order.reconstruct(
                null,
                userId,
                orderItems,
                totalPrice,
                BigDecimal.ZERO, // couponDiscount
                0, // usedPoints
                totalPrice, // finalAmount
                null, // couponId
                OrderStatus.PENDING,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
