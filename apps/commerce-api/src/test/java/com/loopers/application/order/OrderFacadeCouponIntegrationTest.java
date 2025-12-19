package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponDomainService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentRepository;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderFacadeCouponIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private OrderDomainService orderDomainService;

    @Autowired
    private PaymentDomainService paymentDomainService;

    @Autowired
    private PointAccountDomainService pointAccountDomainService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponDomainService couponDomainService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Nested
    @DisplayName("쿠폰 적용 주문")
    class CouponOrderTest {

        @Test
        @DisplayName("쿠폰 적용 시 할인된 금액으로 결제된다")
        void test1() {
            // arrange
            String userId = "coupon-test-user-1";
            pointAccountDomainService.createForUser(userId);
            pointAccountDomainService.charge(userId, 100000L);

            long productPrice = 10000L;
            long quantity = 2L;
            long totalAmount = productPrice * quantity;

            Product product = Product.create("Test Product", "Test Description", productPrice, 100L, null);
            productRepository.save(product);

            Coupon coupon = couponDomainService.issueCoupon(userId, "테스트 쿠폰", 5000L);

            List<OrderDto.OrderItemRequest> items = List.of(
                new OrderDto.OrderItemRequest(product.getId(), quantity)
            );

            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, coupon.getId());

            // act
            OrderInfo result = orderFacade.createOrder(command);

            // assert
            assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED.name());
            assertThat(result.totalAmount()).isEqualTo(totalAmount);

            Order order = orderDomainService.getOrder(userId, result.orderId());
            assertThat(order.getCouponId()).isEqualTo(coupon.getId());
            assertThat(order.getDiscountAmount()).isEqualTo(5000L);
            assertThat(order.getPaymentAmount()).isEqualTo(15000L);

            Payment payment = paymentDomainService.getPaymentByOrderId(result.orderId());
            assertThat(payment.getAmount()).isEqualTo(15000L);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("쿠폰 금액이 주문 금액보다 크면 주문 금액만큼만 할인되고 Payment는 생성되지 않는다")
        void test2() {
            // arrange
            String userId = "coupon-test-user-2";
            pointAccountDomainService.createForUser(userId);
            pointAccountDomainService.charge(userId, 100000L);

            long productPrice = 1000L;
            long quantity = 1L;

            Product product = Product.create("Cheap Product", "Test Description", productPrice, 100L, null);
            productRepository.save(product);

            Coupon coupon = couponDomainService.issueCoupon(userId, "큰 할인 쿠폰", 10000L);

            List<OrderDto.OrderItemRequest> items = List.of(
                new OrderDto.OrderItemRequest(product.getId(), quantity)
            );

            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, coupon.getId());

            // act
            OrderInfo result = orderFacade.createOrder(command);

            // assert
            assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED.name());

            Order order = orderDomainService.getOrder(userId, result.orderId());
            assertThat(order.getDiscountAmount()).isEqualTo(1000L);
            assertThat(order.getPaymentAmount()).isEqualTo(0L);

            assertThat(paymentRepository.findByOrderId(result.orderId())).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 쿠폰은 사용할 수 없다")
        void test3() {
            // arrange
            String userId = "coupon-test-user-3";
            String otherUserId = "other-user";
            pointAccountDomainService.createForUser(userId);
            pointAccountDomainService.charge(userId, 100000L);

            Product product = Product.create("Test Product", "Test Description", 10000L, 100L, null);
            productRepository.save(product);

            Coupon otherUserCoupon = couponDomainService.issueCoupon(otherUserId, "다른 유저 쿠폰", 5000L);

            List<OrderDto.OrderItemRequest> items = List.of(
                new OrderDto.OrderItemRequest(product.getId(), 1L)
            );

            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, otherUserCoupon.getId());

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(command))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("해당 쿠폰의 소유자가 아닙니다");
        }

        @Test
        @DisplayName("이미 사용된 쿠폰은 사용할 수 없다")
        void test4() {
            // arrange
            String userId = "coupon-test-user-4";
            pointAccountDomainService.createForUser(userId);
            pointAccountDomainService.charge(userId, 100000L);

            Product product = Product.create("Test Product", "Test Description", 10000L, 100L, null);
            productRepository.save(product);

            Coupon coupon = couponDomainService.issueCoupon(userId, "테스트 쿠폰", 5000L);
            couponDomainService.useCoupon(coupon.getId());

            List<OrderDto.OrderItemRequest> items = List.of(
                new OrderDto.OrderItemRequest(product.getId(), 1L)
            );

            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, coupon.getId());

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(command))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("이미 사용된 쿠폰입니다");
        }

        @Test
        @DisplayName("쿠폰 없이 주문하면 할인 없이 전액 결제")
        void test5() {
            // arrange
            String userId = "coupon-test-user-5";
            pointAccountDomainService.createForUser(userId);
            pointAccountDomainService.charge(userId, 100000L);

            long productPrice = 10000L;
            long quantity = 2L;

            Product product = Product.create("Test Product", "Test Description", productPrice, 100L, null);
            productRepository.save(product);

            List<OrderDto.OrderItemRequest> items = List.of(
                new OrderDto.OrderItemRequest(product.getId(), quantity)
            );

            OrderCreateCommand command = OrderCreateCommand.forPointPayment(userId, items);

            // act
            OrderInfo result = orderFacade.createOrder(command);

            // assert
            Order order = orderDomainService.getOrder(userId, result.orderId());
            assertThat(order.getCouponId()).isNull();
            assertThat(order.getDiscountAmount()).isEqualTo(0L);
            assertThat(order.getPaymentAmount()).isEqualTo(20000L);
        }
    }
}
