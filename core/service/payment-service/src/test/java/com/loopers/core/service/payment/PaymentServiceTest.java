package com.loopers.core.service.payment;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PgPaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.payment.command.PaymentCommand;
import com.loopers.core.service.payment.component.OrderLineAggregator;
import com.loopers.core.service.payment.component.PayAmountDiscountStrategy;
import com.loopers.core.service.payment.component.PayAmountDiscountStrategySelector;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("결제 서비스")
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderLineAggregator orderLineAggregator;

    @Mock
    private PayAmountDiscountStrategySelector discountStrategySelector;

    @Mock
    private PgClient pgClient;

    @InjectMocks
    private PaymentService paymentService;

    @Nested
    @DisplayName("pay() 메서드의 검증 로직")
    class PayValidation {

        @BeforeEach
        void setUp() {
            // @Value 주입 대신 ReflectionTestUtils로 설정
            ReflectionTestUtils.setField(paymentService, "callbackUrl", "http://localhost:8080/callback");
        }

        @Nested
        @DisplayName("이미 결제에 성공한 이력이 있으면")
        class 이미_결제_성공_이력이_있는_경우 {

            @Test
            @DisplayName("예외를 발생시킨다")
            void 예외_발생() {
                // given
                User user = Instancio.create(User.class);
                Order order = Instancio.create(Order.class);
                PaymentCommand command = new PaymentCommand(
                        order.getId().value(),
                        user.getIdentifier().value(),
                        "CREDIT",
                        "1234567890123456",
                        null
                );

                when(userRepository.getByIdentifier(any(UserIdentifier.class)))
                        .thenReturn(user);
                when(orderRepository.getById(any()))
                        .thenReturn(order);

                // 이미 성공한 결제 존재
                when(paymentRepository.findBy(order.getOrderKey(), PgPaymentStatus.SUCCESS))
                        .thenReturn(Optional.of(Instancio.create(Payment.class)));

                // when & then
                assertThatThrownBy(() -> paymentService.pay(command))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제에 성공한 이력이 있는 주문입니다.");
            }
        }

        @Nested
        @DisplayName("이미 결제가 진행 중이면")
        class 이미_결제_진행_중인_경우 {

            @Test
            @DisplayName("예외를 발생시킨다")
            void 예외_발생() {
                // given
                User user = Instancio.create(User.class);
                Order order = Instancio.create(Order.class);
                PaymentCommand command = new PaymentCommand(
                        order.getId().value(),
                        user.getIdentifier().value(),
                        "CREDIT",
                        "1234567890123456",
                        null
                );

                when(userRepository.getByIdentifier(any(UserIdentifier.class)))
                        .thenReturn(user);
                when(orderRepository.getById(any()))
                        .thenReturn(order);

                // 성공한 결제는 없음
                when(paymentRepository.findBy(order.getOrderKey(), PgPaymentStatus.SUCCESS))
                        .thenReturn(Optional.empty());

                // 진행 중인 결제 존재 (Lock 적용)
                when(paymentRepository.findByWithLock(order.getOrderKey(), PgPaymentStatus.PENDING))
                        .thenReturn(Optional.of(Instancio.create(Payment.class)));

                // when & then
                assertThatThrownBy(() -> paymentService.pay(command))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("이미 결제가 진행 중인 주문입니다.");
            }
        }

        @Nested
        @DisplayName("결제 가능한 상태이면")
        class 결제_가능한_상태 {

            @Test
            @DisplayName("정상적으로 결제가 진행된다")
            void 정상_결제_진행() {
                // given
                User user = Instancio.create(User.class);
                Order order = Instancio.create(Order.class);
                List<OrderItem> orderItems = Instancio.createList(OrderItem.class);
                PayAmount payAmount = Instancio.create(PayAmount.class);
                PaymentCommand command = new PaymentCommand(
                        order.getId().value(),
                        user.getIdentifier().value(),
                        "CREDIT",
                        "1234567890123456",
                        null
                );

                when(userRepository.getByIdentifier(any(UserIdentifier.class)))
                        .thenReturn(user);
                when(orderRepository.getById(any()))
                        .thenReturn(order);
                when(orderItemRepository.findAllByOrderId(any()))
                        .thenReturn(orderItems);

                // 성공한 결제 없음
                when(paymentRepository.findBy(order.getOrderKey(), PgPaymentStatus.SUCCESS))
                        .thenReturn(Optional.empty());

                // 진행 중인 결제 없음
                when(paymentRepository.findByWithLock(order.getOrderKey(), PgPaymentStatus.PENDING))
                        .thenReturn(Optional.empty());

                // Discount Strategy Mock
                PayAmountDiscountStrategy strategy = mock(PayAmountDiscountStrategy.class);
                when(strategy.discount(any(), any()))
                        .thenReturn(payAmount);
                when(discountStrategySelector.select(any()))
                        .thenReturn(strategy);

                when(orderLineAggregator.aggregate(any()))
                        .thenReturn(payAmount);
                when(pgClient.pay(any(), anyString()))
                        .thenReturn(new PgPayment(new TransactionKey("TXN_123"), PgPaymentStatus.PENDING, FailedReason.empty()));
                when(paymentRepository.save(any()))
                        .thenAnswer(invocation -> invocation.getArgument(0));

                // when
                Payment result = paymentService.pay(command);

                // then
                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getStatus()).isEqualTo(PgPaymentStatus.PENDING);
                });
            }
        }
    }
}
