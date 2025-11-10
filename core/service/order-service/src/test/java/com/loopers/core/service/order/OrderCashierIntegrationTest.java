package com.loopers.core.service.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderedProduct;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.*;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.order.component.OrderCashier;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@DisplayName("OrderCashier 통합 테스트")
class OrderCashierIntegrationTest extends IntegrationTest {

    @Autowired
    private OrderCashier orderCashier;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User user;
    private Order order;
    private PayAmount payAmount;

    @BeforeEach
    void setUp() {
        // 사용자 생성 및 저장
        user = User.create(
                new UserIdentifier("testuser"),
                new UserEmail("test@example.com"),
                new UserBirthDay(LocalDate.of(2000, 1, 1)),
                UserGender.MALE
        );
        user = userRepository.save(user);

        // 사용자 포인트 생성 및 저장 (50,000 포인트)
        UserPoint userPoint = UserPoint.mappedBy(
                UserPointId.empty(),
                user.getUserId(),
                new UserPointBalance(new BigDecimal("50000")),
                CreatedAt.now(),
                UpdatedAt.now()
        );
        userPointRepository.save(userPoint);

        // 주문 생성
        order = Order.create(
                user.getUserId(),
                List.of(new OrderedProduct(new ProductId("1"), new Quantity(2L)))
        );

        // 결제 금액
        payAmount = new PayAmount(new BigDecimal("20000"));
    }

    @Nested
    @DisplayName("checkout 메서드")
    class CheckoutMethod {

        @Test
        @DisplayName("주문을 생성하고 포인트를 차감하고 결제 정보를 저장한다")
        void checkoutSuccess() {
            // when
            Order result = orderCashier.checkout(user, order, payAmount);

            // then - 주문 저장 확인
            Order savedOrder = orderRepository.save(order);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getUserId()).isEqualTo(user.getUserId());
                softly.assertThat(result.getOrderedProducts()).hasSize(1);
                softly.assertThat(result.getOrderedProducts().get(0).getProductId().value()).isEqualTo("1");
                softly.assertThat(result.getOrderedProducts().get(0).getQuantity().value()).isEqualTo(2L);
            });

            // 포인트 차감 확인
            UserPoint deductedUserPoint = userPointRepository.getByUserId(user.getUserId());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(deductedUserPoint.getBalance().value())
                        .isEqualByComparingTo(new BigDecimal("30000")); // 50000 - 20000
            });

            // 결제 정보 저장 확인
            Payment payment = Payment.create(result.getOrderId(), user.getUserId(), payAmount);
            Payment savedPayment = paymentRepository.save(payment);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedPayment).isNotNull();
                softly.assertThat(savedPayment.getOrderId()).isEqualTo(result.getOrderId());
                softly.assertThat(savedPayment.getUserId()).isEqualTo(user.getUserId());
                softly.assertThat(savedPayment.getAmount().value()).isEqualByComparingTo(new BigDecimal("20000"));
            });
        }

        @Test
        @DisplayName("포인트가 부족하면 예외를 발생시킨다")
        void throwExceptionWhenInsufficientPoint() {
            // given
            PayAmount largePayAmount = new PayAmount(new BigDecimal("100000"));

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> orderCashier.checkout(user, order, largePayAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자의 포인트 잔액이 충분하지 않습니다.");
        }

        @Test
        @DisplayName("정확히 남은 포인트만큼 결제하면 포인트가 0이 된다")
        void checkoutWithExactPoint() {
            // given
            PayAmount exactPayAmount = new PayAmount(new BigDecimal("50000"));

            // when
            orderCashier.checkout(user, order, exactPayAmount);

            // then
            UserPoint userPoint = userPointRepository.getByUserId(user.getUserId());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(userPoint.getBalance().value()).isZero();
            });
        }
    }
}
