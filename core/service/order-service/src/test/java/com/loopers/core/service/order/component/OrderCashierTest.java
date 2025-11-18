package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.vo.*;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

@DisplayName("OrderCashier 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OrderCashierTest {

    @Mock
    private UserPointRepository userPointRepository;

    @InjectMocks
    private OrderCashier orderCashier;

    @Nested
    @DisplayName("checkout 메서드")
    class CheckoutMethod {

        private User user;
        private Order order;

        @BeforeEach
        void setUp() {
            user = Instancio.of(User.class)
                    .set(field(User::getId), UserId.empty())
                    .set(field(User::getIdentifier), new UserIdentifier("kilian"))
                    .set(field(User::getEmail), new UserEmail("kilian@gmail.com"))
                    .create();

            order = Instancio.of(Order.class)
                    .set(field(Order::getId), OrderId.empty())
                    .set(field(Order::getUserId), user.getId())
                    .create();
        }

        @Nested
        @DisplayName("포인트가 부족할 때")
        class WhenInsufficientPoint {

            private UserPoint userPoint;
            private PayAmount largePayAmount;

            @BeforeEach
            void setUp() {
                userPoint = Instancio.of(UserPoint.class)
                        .set(field("id"), UserPointId.empty())
                        .set(field("userId"), user.getId())
                        .set(field("balance"), new UserPointBalance(new BigDecimal(50_000)))
                        .create();

                largePayAmount = new PayAmount(new BigDecimal("100000"));

                when(userPointRepository.getByUserIdWithLock(user.getId())).thenReturn(userPoint);
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void throwExceptionWhenInsufficientPoint() {
                // when & then
                assertThatThrownBy(() -> orderCashier.checkout(user, order, largePayAmount, CouponId.empty()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자의 포인트 잔액이 충분하지 않습니다.");
            }
        }
    }
}
