package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponDomainService;
import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.fixture.TestEventCaptor;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.order.PaymentJpaRepository;
import com.loopers.infrastructure.point.PointAccountJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.order.OrderDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderFacade의 이벤트 발행을 검증하는 테스트.
 *
 * 주의: @Transactional을 사용하지 않음
 * - OrderFacade는 runAfterCommit()으로 커밋 후 이벤트를 발행
 * - @Transactional 테스트는 롤백되므로 afterCommit이 실행되지 않음
 * - 따라서 실제 커밋이 필요하고, AfterEach에서 데이터 정리
 */
@SpringBootTest
@Import(TestEventCaptor.class)
class OrderFacadeEventPublishTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PointAccountDomainService pointAccountDomainService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponDomainService couponDomainService;

    @Autowired
    private TestEventCaptor eventCaptor;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private PointAccountJpaRepository pointAccountJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        eventCaptor.clear();
    }

    @AfterEach
    void tearDown() {
        paymentJpaRepository.deleteAll();
        orderJpaRepository.deleteAll();
        couponJpaRepository.deleteAll();
        pointAccountJpaRepository.deleteAll();
        if (testProduct != null) {
            productJpaRepository.deleteById(testProduct.getId());
            testProduct = null;
        }
    }

    private Product createAndSaveProduct(String name, long price, long stock) {
        Product product = Product.create(name, "Description", price, stock, null);
        testProduct = productRepository.save(product);
        return testProduct;
    }

    private void createUserWithPoint(String userId, long point) {
        pointAccountDomainService.createForUser(userId);
        if (point > 0) {
            pointAccountDomainService.charge(userId, point);
        }
    }

    @Nested
    @DisplayName("OrderCompletedEvent 발행 검증")
    class OrderCompletedEventTest {

        @Test
        @DisplayName("포인트 결제 주문 완료 시 OrderCompletedEvent가 발행된다")
        void orderCompletedEventTest1() {
            // arrange
            String userId = "event-test-user-1";
            createUserWithPoint(userId, 100000L);
            Product product = createAndSaveProduct("Test Product", 10000L, 100L);

            List<OrderDto.OrderItemRequest> items = List.of(
                    new OrderDto.OrderItemRequest(product.getId(), 2L)
            );
            OrderCreateCommand command = OrderCreateCommand.forPointPayment(userId, items);

            // act
            OrderInfo result = orderFacade.createOrder(command);

            // assert
            assertThat(eventCaptor.hasEventOfType(OrderCompletedEvent.class)).isTrue();

            OrderCompletedEvent event = eventCaptor.getFirstEventOfType(OrderCompletedEvent.class);
            assertThat(event.getOrderId()).isEqualTo(result.orderId());
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getTotalAmount()).isEqualTo(20000L);
            assertThat(event.getPaymentAmount()).isEqualTo(20000L);
        }

        @Test
        @DisplayName("무료 주문(쿠폰 전액 할인) 시에도 OrderCompletedEvent가 발행된다")
        void orderCompletedEventTest2() {
            // arrange
            String userId = "event-test-user-2";
            createUserWithPoint(userId, 0L);
            Product product = createAndSaveProduct("Cheap Product", 1000L, 100L);

            Coupon coupon = couponDomainService.issueCoupon(userId, "큰 할인 쿠폰", 10000L);

            List<OrderDto.OrderItemRequest> items = List.of(
                    new OrderDto.OrderItemRequest(product.getId(), 1L)
            );
            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, coupon.getId());

            // act
            OrderInfo result = orderFacade.createOrder(command);

            // assert
            assertThat(eventCaptor.hasEventOfType(OrderCompletedEvent.class)).isTrue();

            OrderCompletedEvent event = eventCaptor.getFirstEventOfType(OrderCompletedEvent.class);
            assertThat(event.getOrderId()).isEqualTo(result.orderId());
            assertThat(event.getPaymentAmount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("CouponUsedEvent 발행 검증")
    class CouponUsedEventTest {

        @Test
        @DisplayName("쿠폰 적용 주문 시 CouponUsedEvent가 발행된다")
        void couponUsedEventTest1() {
            // arrange
            String userId = "event-test-user-3";
            createUserWithPoint(userId, 100000L);
            Product product = createAndSaveProduct("Test Product", 10000L, 100L);

            Coupon coupon = couponDomainService.issueCoupon(userId, "테스트 쿠폰", 3000L);

            List<OrderDto.OrderItemRequest> items = List.of(
                    new OrderDto.OrderItemRequest(product.getId(), 1L)
            );
            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, coupon.getId());

            // act
            OrderInfo result = orderFacade.createOrder(command);

            // assert
            assertThat(eventCaptor.hasEventOfType(CouponUsedEvent.class)).isTrue();

            CouponUsedEvent event = eventCaptor.getFirstEventOfType(CouponUsedEvent.class);
            assertThat(event.getCouponId()).isEqualTo(coupon.getId());
            assertThat(event.getOrderId()).isEqualTo(result.orderId());
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getDiscountAmount()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("쿠폰 없이 주문하면 CouponUsedEvent가 발행되지 않는다")
        void couponUsedEventTest2() {
            // arrange
            String userId = "event-test-user-4";
            createUserWithPoint(userId, 100000L);
            Product product = createAndSaveProduct("Test Product", 10000L, 100L);

            List<OrderDto.OrderItemRequest> items = List.of(
                    new OrderDto.OrderItemRequest(product.getId(), 1L)
            );
            OrderCreateCommand command = OrderCreateCommand.forPointPayment(userId, items);

            // act
            orderFacade.createOrder(command);

            // assert
            assertThat(eventCaptor.hasEventOfType(CouponUsedEvent.class)).isFalse();
        }

        @Test
        @DisplayName("쿠폰 금액이 주문 금액보다 커도 CouponUsedEvent가 발행된다")
        void couponUsedEventTest3() {
            // arrange
            String userId = "event-test-user-5";
            createUserWithPoint(userId, 0L);
            Product product = createAndSaveProduct("Cheap Product", 1000L, 100L);

            Coupon coupon = couponDomainService.issueCoupon(userId, "큰 할인 쿠폰", 10000L);

            List<OrderDto.OrderItemRequest> items = List.of(
                    new OrderDto.OrderItemRequest(product.getId(), 1L)
            );
            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, coupon.getId());

            // act
            orderFacade.createOrder(command);

            // assert
            assertThat(eventCaptor.hasEventOfType(CouponUsedEvent.class)).isTrue();

            CouponUsedEvent event = eventCaptor.getFirstEventOfType(CouponUsedEvent.class);
            assertThat(event.getDiscountAmount()).isEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("이벤트 발행 순서 및 개수 검증")
    class EventOrderTest {

        @Test
        @DisplayName("쿠폰 적용 주문 시 CouponUsedEvent와 OrderCompletedEvent가 모두 발행된다")
        void eventOrderTest1() {
            // arrange
            String userId = "event-test-user-6";
            createUserWithPoint(userId, 100000L);
            Product product = createAndSaveProduct("Test Product", 10000L, 100L);

            Coupon coupon = couponDomainService.issueCoupon(userId, "테스트 쿠폰", 3000L);

            List<OrderDto.OrderItemRequest> items = List.of(
                    new OrderDto.OrderItemRequest(product.getId(), 1L)
            );
            OrderCreateCommand command = OrderCreateCommand.forPointPaymentWithCoupon(userId, items, coupon.getId());

            // act
            orderFacade.createOrder(command);

            // assert
            assertThat(eventCaptor.countEventsOfType(CouponUsedEvent.class)).isEqualTo(1);
            assertThat(eventCaptor.countEventsOfType(OrderCompletedEvent.class)).isEqualTo(1);
        }
    }
}
