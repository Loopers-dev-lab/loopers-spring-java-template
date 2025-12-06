package com.loopers.interfaces.api.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.order.PaymentRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("결제 콜백 통합 테스트")
class PaymentCallbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String TRANSACTION_KEY = "20250816:TR:9577c5";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("PG 성공 콜백을 받으면 결제 상태가 SUCCESS로 변경되고 Order도 CONFIRMED로 변경된다")
    void test1() throws Exception {
        // arrange - Order와 Payment 생성
        OrderItem item = OrderItem.create(1L, "상품", 1L, 10000L);
        Order order = Order.create("user1", List.of(item), 10000L);
        order.startPayment();  // PAYING 상태로 변경
        order = orderRepository.save(order);

        Payment payment = Payment.create(order.getId(), "user1", 10000L, com.loopers.domain.order.PaymentType.CARD_ONLY);
        payment.updatePgTransactionId(TRANSACTION_KEY);
        payment = paymentRepository.save(payment);

        String requestBody = """
                {
                    "transactionKey": "%s",
                    "status": "SUCCESS",
                    "reason": null
                }
                """.formatted(TRANSACTION_KEY);

        // act
        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // assert - Payment와 Order 모두 확인
        Payment savedPayment = paymentRepository.findByPgTransactionId(TRANSACTION_KEY).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("PG 실패 콜백을 받으면 결제 상태가 FAILED로 변경되고 Order도 FAILED로 변경된다")
    void test2() throws Exception {
        // arrange - Product, Order, Payment 생성
        Product product = Product.create("상품", "설명", 10000L, 100L, 1L);
        product = productRepository.save(product);

        OrderItem item = OrderItem.create(product.getId(), "상품", 1L, 10000L);
        Order order = Order.create("user1", List.of(item), 10000L);
        order.startPayment();  // PAYING 상태로 변경
        order = orderRepository.save(order);

        Payment payment = Payment.create(order.getId(), "user1", 10000L, com.loopers.domain.order.PaymentType.CARD_ONLY);
        payment.updatePgTransactionId(TRANSACTION_KEY);
        payment = paymentRepository.save(payment);

        String requestBody = """
                {
                    "transactionKey": "%s",
                    "status": "FAILED",
                    "reason": "한도 초과"
                }
                """.formatted(TRANSACTION_KEY);

        // act
        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // assert - Payment와 Order 모두 확인
        Payment savedPayment = paymentRepository.findByPgTransactionId(TRANSACTION_KEY).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getFailureReason()).isEqualTo("한도 초과");

        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("transactionKey가 null이면 400 Bad Request")
    void test3() throws Exception {
        String requestBody = """
                {
                    "transactionKey": null,
                    "status": "SUCCESS",
                    "reason": null
                }
                """;

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("transactionKey가 빈 문자열이면 400 Bad Request")
    void test4() throws Exception {
        String requestBody = """
                {
                    "transactionKey": "",
                    "status": "SUCCESS",
                    "reason": null
                }
                """;

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("status가 null이면 400 Bad Request")
    void test5() throws Exception {
        String requestBody = """
                {
                    "transactionKey": "TX-12345",
                    "status": null,
                    "reason": null
                }
                """;

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("status가 빈 문자열이면 400 Bad Request")
    void test6() throws Exception {
        String requestBody = """
                {
                    "transactionKey": "TX-12345",
                    "status": "",
                    "reason": null
                }
                """;

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
