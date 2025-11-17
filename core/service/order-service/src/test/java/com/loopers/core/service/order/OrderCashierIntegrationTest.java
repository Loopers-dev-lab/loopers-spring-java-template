package com.loopers.core.service.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.*;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.order.component.OrderCashier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("주문 결제 처리")
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

    @Nested
    @DisplayName("결제 시")
    class 결제_시 {

        @Nested
        @DisplayName("포인트가 충분한 경우")
        class 포인트가_충분한_경우 {

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
                        user.getId(),
                        new UserPointBalance(new BigDecimal("50000")),
                        CreatedAt.now(),
                        UpdatedAt.now()
                );
                userPointRepository.save(userPoint);

                // 주문 생성 및 저장
                order = Order.create(user.getId());
                order = orderRepository.save(order);

                // 결제 금액 (20,000 - 포인트는 50,000으로 충분함)
                payAmount = new PayAmount(new BigDecimal("20000"));
            }

            @Test
            @DisplayName("주문을 생성하고 포인트를 차감하고 결제 정보를 저장한다")
            void 주문을_생성하고_포인트를_차감하고_결제_정보를_저장한다() {
                // when
                Payment result = orderCashier.checkout(user, order, payAmount);

                // then - 결제 정보 확인
                assertSoftly(softly -> {
                    softly.assertThat(result)
                            .as("결제 정보")
                            .isNotNull();
                    softly.assertThat(result.getOrderId())
                            .as("주문 ID 일치")
                            .isEqualTo(order.getId());
                    softly.assertThat(result.getUserId())
                            .as("사용자 ID 일치")
                            .isEqualTo(user.getId());
                    softly.assertThat(result.getAmount().value())
                            .as("결제 금액")
                            .isEqualByComparingTo(new BigDecimal("20000"));
                });

                // 포인트 차감 확인
                UserPoint deductedUserPoint = userPointRepository.getByUserId(user.getId());
                assertSoftly(softly -> {
                    softly.assertThat(deductedUserPoint.getBalance().value())
                            .as("차감 후 포인트 잔액")
                            .isEqualByComparingTo(new BigDecimal("30000")); // 50000 - 20000
                });
            }

            @Test
            @DisplayName("정확히 남은 포인트만큼 결제하면 포인트가 0이 된다")
            void 정확히_남은_포인트만큼_결제하면_포인트가_0이_된다() {
                // given
                PayAmount exactPayAmount = new PayAmount(new BigDecimal("50000"));

                // when
                orderCashier.checkout(user, order, exactPayAmount);

                // then
                UserPoint userPoint = userPointRepository.getByUserId(user.getId());
                assertSoftly(softly -> {
                    softly.assertThat(userPoint.getBalance().value())
                            .as("차감 후 포인트 잔액")
                            .isZero();
                });
            }

            @Test
            @DisplayName("동시에 여러 주문요청을 결제한다면 결제한 금액만큼 감소된다.")
            void 동시에_여러_주문요청을_결제한다면_결제한_금액만큼_감소된다() throws InterruptedException {
                int requestCount = 100;
                PayAmount payAmountPerUser = new PayAmount(new BigDecimal("500"));
                List<Payment> results = ConcurrencyTestUtil.executeInParallel(
                        requestCount,
                        index -> orderCashier.checkout(user, order, payAmountPerUser)
                );

                UserPoint actualUserPoint = userPointRepository.getByUserId(user.getId());
                assertSoftly(softly -> {
                    softly.assertThat(results).as("동시 요청 결과 수").hasSize(requestCount);
                    softly.assertThat(actualUserPoint.getBalance().value()).isEqualByComparingTo(new BigDecimal(0));
                });
            }
        }

        @Nested
        @DisplayName("포인트가 부족한 경우")
        class 포인트가_부족한_경우 {

            private User user;
            private Order order;

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
                        user.getId(),
                        new UserPointBalance(new BigDecimal("50000")),
                        CreatedAt.now(),
                        UpdatedAt.now()
                );
                userPointRepository.save(userPoint);

                // 주문 생성 및 저장
                order = Order.create(user.getId());
                order = orderRepository.save(order);
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void 예외를_발생시킨다() {
                // given
                PayAmount largePayAmount = new PayAmount(new BigDecimal("100000"));

                // when & then
                org.assertj.core.api.Assertions.assertThatThrownBy(
                                () -> orderCashier.checkout(user, order, largePayAmount)
                        ).isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("사용자의 포인트 잔액이 충분하지 않습니다.");
            }
        }
    }
}
