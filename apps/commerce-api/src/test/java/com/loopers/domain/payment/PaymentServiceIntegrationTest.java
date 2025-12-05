package com.loopers.domain.payment;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.application.payment.PaymentPgCardCommand;
import com.loopers.application.payment.PaymentPointCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.create("testuser", "test@mail.com", "1990-01-01", Gender.MALE);
        userRepository.save(testUser);

        Point point = Point.create(testUser.getId(), 100000L);
        pointRepository.save(point);

        Brand brand = Brand.create("Test Brand");
        brandRepository.save(brand);

        testProduct = Product.create("Test Product", 10000L, 100, brand);
        productRepository.save(testProduct);
    }

    @Nested
    @DisplayName("포인트 결제 전체 흐름")
    class PayWithPointFlow {

        @DisplayName("포인트 결제가 성공하면 Order 상태가 COMPLETED가 된다.")
        @Test
        void payWithPoint_success() {
            // given
            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(testProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.POINT,
                    null
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            PaymentPointCommand paymentCommand = PaymentPointCommand.of(
                    testUser.getUserIdValue(),
                    orderInfo.orderId(),
                    0L,
                    null,
                    "point-key-" + System.currentTimeMillis()
            );

            // when
            PaymentInfo paymentInfo = paymentFacade.payWithPoint(paymentCommand);

            // then
            assertThat(paymentInfo.status()).isEqualTo(PaymentStatus.SUCCESS);

            Order order = orderRepository.findById(orderInfo.orderId()).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @DisplayName("중복된 멱등성 키로 결제 요청 시 기존 결제를 반환한다.")
        @Test
        void payWithPoint_idempotency() {
            // given
            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(testProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.POINT,
                    null
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            String idempotencyKey = "duplicate-key-" + System.currentTimeMillis();
            PaymentPointCommand paymentCommand = PaymentPointCommand.of(
                    testUser.getUserIdValue(),
                    orderInfo.orderId(),
                    0L,
                    null,
                    idempotencyKey
            );

            // when
            PaymentInfo firstPayment = paymentFacade.payWithPoint(paymentCommand);
            PaymentInfo secondPayment = paymentFacade.payWithPoint(paymentCommand);

            // then
            assertThat(secondPayment.paymentId()).isEqualTo(firstPayment.paymentId());
        }

        @DisplayName("포인트 부족 시 예외가 발생한다.")
        @Test
        void payWithPoint_insufficientBalance() {
            // given
            Product expensiveProduct = Product.create("Expensive", 200000L, 10, testProduct.getBrand());
            productRepository.save(expensiveProduct);

            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(expensiveProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.POINT,
                    null
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            PaymentPointCommand paymentCommand = PaymentPointCommand.of(
                    testUser.getUserIdValue(),
                    orderInfo.orderId(),
                    0L,
                    null,
                    "point-key-" + System.currentTimeMillis()
            );

            // when & then
            assertThatThrownBy(() -> paymentFacade.payWithPoint(paymentCommand))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("포인트");
        }
    }

    @Nested
    @DisplayName("PG 카드 결제 전체 흐름")
    class PayWithPgCardFlow {

        @DisplayName("PG 카드 결제 요청이 성공하면 Order 상태가 PAYMENT_PENDING이 된다.")
        @Test
        void payWithPgCard_pending() {
            // given
            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(testProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.PG_CARD,
                    new OrderPlaceCommand.CardInfo("SAMSUNG", "1234-5678-9012-3456")
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            PaymentPgCardCommand paymentCommand = PaymentPgCardCommand.of(
                    testUser.getUserIdValue(),
                    orderInfo.orderId(),
                    "SAMSUNG",
                    "1234-5678-9012-3456",
                    0L,
                    null,
                    "card-key-" + System.currentTimeMillis()
            );

            // when
            PaymentInfo paymentInfo = paymentFacade.payWithPgCard(paymentCommand);

            // then
            assertThat(paymentInfo.status()).isEqualTo(PaymentStatus.PENDING);

            Order order = orderRepository.findById(orderInfo.orderId()).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        }
    }
}
