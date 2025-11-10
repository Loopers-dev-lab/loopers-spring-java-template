package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderedProduct;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.domain.user.vo.UserPointBalance;
import com.loopers.core.domain.user.vo.UserPointId;
import com.loopers.core.domain.user.type.UserGender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("주문 계산원")
@ExtendWith(MockitoExtension.class)
class OrderCashierTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private OrderCashier orderCashier;

    private User user;
    private Order order;
    private PayAmount payAmount;
    private UserPoint userPoint;

    @BeforeEach
    void setUp() {
        // given
        user = User.create(
            new UserIdentifier("testuser"),
            new UserEmail("test@example.com"),
            new UserBirthDay(LocalDate.of(2000, 1, 1)),
            UserGender.MALE
        );

        order = Order.create(
            user.getUserId(),
            List.of(new OrderedProduct(new ProductId("1"), new Quantity(2L)))
        );

        payAmount = new PayAmount(new BigDecimal("20000"));

        userPoint = UserPoint.mappedBy(
            UserPointId.empty(),
            user.getUserId(),
            new UserPointBalance(new BigDecimal("50000")),
            null,
            null
        );
    }

    @Nested
    @DisplayName("checkout 메서드")
    class CheckoutMethod {

        @Test
        @DisplayName("포인트를 차감하고 Order와 Payment를 저장한다")
        void checkoutSuccess() {
            // given
            when(userPointRepository.getByUserId(user.getUserId())).thenReturn(userPoint);
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // when
            Order result = orderCashier.checkout(user, order, payAmount);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(user.getUserId());

            // 포인트 차감 검증
            ArgumentCaptor<UserPoint> userPointCaptor = ArgumentCaptor.forClass(UserPoint.class);
            verify(userPointRepository).save(userPointCaptor.capture());
            assertThat(userPointCaptor.getValue().getBalance().value())
                .isEqualByComparingTo(new BigDecimal("30000")); // 50000 - 20000

            // Order와 Payment 저장 검증
            verify(orderRepository).save(any(Order.class));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("포인트가 부족하면 예외를 발생시킨다")
        void throwExceptionWhenInsufficientPoint() {
            // given
            PayAmount largePayAmount = new PayAmount(new BigDecimal("100000"));

            when(userPointRepository.getByUserId(user.getUserId())).thenReturn(userPoint);

            // when & then
            assertThatThrownBy(() -> orderCashier.checkout(user, order, largePayAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자의 포인트 잔액이 충분하지 않습니다.");
        }

        @Test
        @DisplayName("정확히 남은 포인트만큼 결제하면 포인트가 0이 된다")
        void checkoutWithExactPoint() {
            // given
            PayAmount exactPayAmount = new PayAmount(new BigDecimal("50000"));

            when(userPointRepository.getByUserId(user.getUserId())).thenReturn(userPoint);
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            // when
            orderCashier.checkout(user, order, exactPayAmount);

            // then
            ArgumentCaptor<UserPoint> userPointCaptor = ArgumentCaptor.forClass(UserPoint.class);
            verify(userPointRepository).save(userPointCaptor.capture());
            assertThat(userPointCaptor.getValue().getBalance().value()).isZero();
        }
    }
}
