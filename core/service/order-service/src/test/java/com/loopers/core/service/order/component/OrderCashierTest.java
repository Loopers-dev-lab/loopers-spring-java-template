package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrderCashier 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OrderCashierTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private OrderCashier orderCashier;

    @Nested
    @DisplayName("checkout 메서드")
    class CheckoutMethod {

        private User user;
        private Order order;

        @BeforeEach
        void setUp() {
            user = User.create(
                    new UserIdentifier("testuser"),
                    new UserEmail("test@example.com"),
                    new UserBirthDay(LocalDate.of(2000, 1, 1)),
                    UserGender.MALE
            );

            order = Order.create(user.getId());
        }

        @Nested
        @DisplayName("포인트가 충분할 때")
        class WhenSufficientPoint {

            private UserPoint userPoint;
            private PayAmount payAmount;

            @BeforeEach
            void setUp() {
                userPoint = UserPoint.mappedBy(
                        UserPointId.empty(),
                        user.getId(),
                        new UserPointBalance(new BigDecimal("50000")),
                        null,
                        null
                );

                payAmount = new PayAmount(new BigDecimal("20000"));

                when(userPointRepository.getByUserIdWithLock(user.getId())).thenReturn(userPoint);
            }

            @Test
            @DisplayName("포인트를 차감하고 Payment를 저장한다")
            void checkoutSuccessAndSavePayment() {
                // when
                Payment result = orderCashier.checkout(user, order, payAmount);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getOrderId()).isEqualTo(order.getId());
                assertThat(result.getUserId()).isEqualTo(user.getId());
                assertThat(result.getAmount()).isEqualTo(payAmount);

                // 포인트 차감 검증
                ArgumentCaptor<UserPoint> userPointCaptor = ArgumentCaptor.forClass(UserPoint.class);
                verify(userPointRepository).save(userPointCaptor.capture());
                assertThat(userPointCaptor.getValue().getBalance().value())
                        .isEqualByComparingTo(new BigDecimal("30000")); // 50000 - 20000

                // Payment 저장 검증
                verify(paymentRepository).save(any(Payment.class));
            }
        }

        @Nested
        @DisplayName("포인트가 부족할 때")
        class WhenInsufficientPoint {

            private UserPoint userPoint;
            private PayAmount largePayAmount;

            @BeforeEach
            void setUp() {
                userPoint = UserPoint.mappedBy(
                        UserPointId.empty(),
                        user.getId(),
                        new UserPointBalance(new BigDecimal("50000")),
                        null,
                        null
                );

                largePayAmount = new PayAmount(new BigDecimal("100000"));

                when(userPointRepository.getByUserIdWithLock(user.getId())).thenReturn(userPoint);
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void throwExceptionWhenInsufficientPoint() {
                // when & then
                assertThatThrownBy(() -> orderCashier.checkout(user, order, largePayAmount))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자의 포인트 잔액이 충분하지 않습니다.");
            }
        }

        @Nested
        @DisplayName("포인트가 정확히 남은 금액과 같을 때")
        class WhenExactPoint {

            private UserPoint userPoint;
            private PayAmount exactPayAmount;

            @BeforeEach
            void setUp() {
                userPoint = UserPoint.mappedBy(
                        UserPointId.empty(),
                        user.getId(),
                        new UserPointBalance(new BigDecimal("50000")),
                        null,
                        null
                );

                exactPayAmount = new PayAmount(new BigDecimal("50000"));

                when(userPointRepository.getByUserIdWithLock(user.getId())).thenReturn(userPoint);
            }

            @Test
            @DisplayName("포인트가 0이 된다")
            void checkoutWithExactPointBecomesZero() {
                // when
                orderCashier.checkout(user, order, exactPayAmount);

                // then
                ArgumentCaptor<UserPoint> userPointCaptor = ArgumentCaptor.forClass(UserPoint.class);
                verify(userPointRepository).save(userPointCaptor.capture());
                assertThat(userPointCaptor.getValue().getBalance().value()).isZero();
            }
        }
    }
}
