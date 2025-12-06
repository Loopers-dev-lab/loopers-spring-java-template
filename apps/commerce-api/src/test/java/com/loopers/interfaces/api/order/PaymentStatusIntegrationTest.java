package com.loopers.interfaces.api.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.domain.order.PaymentType;
import com.loopers.domain.order.PaymentRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("결제 상태 조회 통합 테스트")
class PaymentStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("결제가 PENDING 상태이면 조회 시 PENDING을 반환한다")
    void testPaymentPending() throws Exception {
        // arrange
        OrderItem item = OrderItem.create(1L, "상품", 1L, 10000L);
        Order order = Order.create("user1", List.of(item), 10000L);
        order.startPayment();
        order = orderRepository.save(order);

        Payment payment = Payment.create(order.getId(), "user1", 10000L, PaymentType.CARD_ONLY);
        payment = paymentRepository.save(payment);

        // act & assert
        mockMvc.perform(get("/api/v1/orders/{orderId}/payment", order.getId())
                        .header("X-USER-ID", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId", equalTo(order.getId().intValue())))
                .andExpect(jsonPath("$.data.status", equalTo("PENDING")))
                .andExpect(jsonPath("$.data.paymentType", equalTo("CARD_ONLY")))
                .andExpect(jsonPath("$.data.amount", equalTo(10000)));
    }

    @Test
    @DisplayName("결제가 SUCCESS 상태이면 조회 시 SUCCESS를 반환한다")
    void testPaymentSuccess() throws Exception {
        // arrange
        OrderItem item = OrderItem.create(1L, "상품", 1L, 10000L);
        Order order = Order.create("user1", List.of(item), 10000L);
        order.startPayment();
        order.confirm();
        order = orderRepository.save(order);

        Payment payment = Payment.create(order.getId(), "user1", 10000L, PaymentType.POINT_ONLY);
        payment.markAsSuccess("internal-payment");
        payment = paymentRepository.save(payment);

        // act & assert
        mockMvc.perform(get("/api/v1/orders/{orderId}/payment", order.getId())
                        .header("X-USER-ID", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId", equalTo(order.getId().intValue())))
                .andExpect(jsonPath("$.data.status", equalTo("SUCCESS")))
                .andExpect(jsonPath("$.data.paymentType", equalTo("POINT_ONLY")))
                .andExpect(jsonPath("$.data.amount", equalTo(10000)));
    }

    @Test
    @DisplayName("결제가 FAILED 상태이면 실패 사유를 함께 반환한다")
    void testPaymentFailed() throws Exception {
        // arrange
        OrderItem item = OrderItem.create(1L, "상품", 1L, 10000L);
        Order order = Order.create("user1", List.of(item), 10000L);
        order.startPayment();
        order.fail();
        order = orderRepository.save(order);

        Payment payment = Payment.create(order.getId(), "user1", 10000L, PaymentType.CARD_ONLY);
        payment.markAsFailed("한도 초과");
        payment = paymentRepository.save(payment);

        // act & assert
        mockMvc.perform(get("/api/v1/orders/{orderId}/payment", order.getId())
                        .header("X-USER-ID", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId", equalTo(order.getId().intValue())))
                .andExpect(jsonPath("$.data.status", equalTo("FAILED")))
                .andExpect(jsonPath("$.data.paymentType", equalTo("CARD_ONLY")))
                .andExpect(jsonPath("$.data.amount", equalTo(10000)))
                .andExpect(jsonPath("$.data.failureReason", equalTo("한도 초과")));
    }
}
