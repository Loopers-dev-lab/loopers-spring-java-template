package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.order.Order;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.order.OrderItemJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.payment.PaymentJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentFacadeIntegrationTest {
    @Autowired
    private PaymentFacade paymentFacade;
    @Autowired
    private OrderJpaRepository orderJpaRepository;
    @Autowired
    private OrderItemJpaRepository orderItemJpaRepository;
    @Autowired
    private PaymentJpaRepository paymentJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private PointJpaRepository pointJpaRepository;
    @Autowired
    private ProductJpaRepository productJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("결제 완료 요청")
    @Test
    void requestPaidPayment() {
        // arrange
        User user = userJpaRepository.save(User.create("testUser", "test@test.com", LocalDate.of(2020, 1, 1), Gender.MALE));
        Order order = orderJpaRepository.save(Order.create(user.getId()));
        Payment payment = paymentJpaRepository.save(Payment.create(CardType.SAMSUNG, "1234-1234-1234-1234", 1L, order.getId()));

        // act
        PaymentInfo paymentInfo = paymentFacade.requestPaidPayment(user.getId(), payment.getId());

        // assert
        assertThat(paymentInfo.transactionKey()).isNotNull();
    }
}
