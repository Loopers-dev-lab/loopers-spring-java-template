package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.application.coupon.CouponService;
import com.loopers.application.order.OrderCommand.OrderItemRequest;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@DisplayName("OrderFacade 통합 테스트")
class OrderFacadeIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private com.loopers.domain.order.OrderRepository orderRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private com.loopers.domain.product.ProductRepository productRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성 성공 시나리오")
    @Nested
    class CreateOrderSuccess {

        @DisplayName("쿠폰 없이 주문을 성공적으로 생성할 수 있다")
        @Test
        @Transactional
        void createOrder_withoutCoupon_success() {
            // given
            String userId = "user123";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(50000))
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 2)),
                null // 쿠폰 없음
            );

            // when
            OrderInfo orderInfo = orderFacade.createOrder(userId, command);

            // then
            Order savedOrder = orderRepository.findById(orderInfo.id()).orElseThrow();
            Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            Point updatedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

            assertAll(
                // 주문 정보 검증
                () -> assertThat(savedOrder.getUserId()).isEqualTo(userId),
                () -> assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000)),
                () -> assertThat(savedOrder.getOrderItems()).hasSize(1),
                () -> assertThat(savedOrder.getOrderItems().get(0).getQuantity()).isEqualTo(2),

                // 재고 차감 검증
                () -> assertThat(updatedProduct.getStock()).isEqualTo(8),

                // 포인트 차감 검증
                () -> assertThat(updatedPoint.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(30000))
            );
        }

        @DisplayName("정액 할인 쿠폰을 사용하여 주문을 성공적으로 생성할 수 있다")
        @Test
        @Transactional
        void createOrder_withFixedAmountCoupon_success() {
            // given
            String userId = "user123";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .description("테스트용 쿠폰")
                .build();
            coupon = ((org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>) couponJpaRepository).save(coupon);

            UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
            userCoupon = ((org.springframework.data.jpa.repository.JpaRepository<UserCoupon, Long>) userCouponJpaRepository).save(userCoupon);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(50000))
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 1)),
                userCoupon.getId()
            );

            // when
            OrderInfo orderInfo = orderFacade.createOrder(userId, command);

            // then
            Order savedOrder = orderRepository.findById(orderInfo.id()).orElseThrow();
            UserCoupon updatedUserCoupon = userCouponJpaRepository.findById(userCoupon.getId()).orElseThrow();
            Point updatedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

            assertAll(
                // 주문 정보 검증
                () -> assertThat(savedOrder.getUserId()).isEqualTo(userId),
                () -> assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000)),

                // 쿠폰 사용 검증
                () -> assertThat(updatedUserCoupon.isUsed()).isTrue(),
                () -> assertThat(updatedUserCoupon.getUsedAt()).isNotNull(),

                // 포인트 차감 검증 (10000 - 5000 = 5000 차감)
                () -> assertThat(updatedPoint.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(45000))
            );
        }

        @DisplayName("정률 할인 쿠폰을 사용하여 주문을 성공적으로 생성할 수 있다")
        @Test
        @Transactional
        void createOrder_withPercentageCoupon_success() {
            // given
            String userId = "user123";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Coupon coupon = Coupon.builder()
                .name("20% 할인 쿠폰")
                .type(CouponType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .description("테스트용 쿠폰")
                .build();
            coupon = ((org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>) couponJpaRepository).save(coupon);

            UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
            userCoupon = ((org.springframework.data.jpa.repository.JpaRepository<UserCoupon, Long>) userCouponJpaRepository).save(userCoupon);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(50000))
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 1)),
                userCoupon.getId()
            );

            // when
            OrderInfo orderInfo = orderFacade.createOrder(userId, command);

            // then
            Order savedOrder = orderRepository.findById(orderInfo.id()).orElseThrow();
            UserCoupon updatedUserCoupon = userCouponJpaRepository.findById(userCoupon.getId()).orElseThrow();
            Point updatedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

            assertAll(
                // 주문 정보 검증
                () -> assertThat(savedOrder.getUserId()).isEqualTo(userId),
                () -> assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000)),

                // 쿠폰 사용 검증
                () -> assertThat(updatedUserCoupon.isUsed()).isTrue(),

                // 포인트 차감 검증 (10000 * 0.8 = 8000 차감, 20% 할인)
                () -> assertThat(updatedPoint.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(42000))
            );
        }
    }

    @DisplayName("주문 생성 실패 시나리오")
    @Nested
    class CreateOrderFailure {

        @DisplayName("이미 사용된 쿠폰으로 주문 시 실패한다")
        @Test
        void createOrder_withUsedCoupon_fails() {
            // given
            String userId = "user123";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .description("테스트용 쿠폰")
                .build();
            coupon = ((org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>) couponJpaRepository).save(coupon);

            UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
            userCoupon = ((org.springframework.data.jpa.repository.JpaRepository<UserCoupon, Long>) userCouponJpaRepository).save(userCoupon);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(50000))
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            // 쿠폰을 먼저 사용
            userCoupon.use();
            userCouponJpaRepository.save(userCoupon);

            OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 1)),
                userCoupon.getId()
            );

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(userId, command)
            );

            assertThat(exception.getMessage()).contains("사용할 수 없는 쿠폰입니다");

            // 롤백 확인: 상품 재고와 포인트가 변경되지 않았는지 확인
            Product unchangedProduct = productRepository.findById(product.getId()).orElseThrow();
            Point unchangedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

            assertAll(
                () -> assertThat(unchangedProduct.getStock()).isEqualTo(10),
                () -> assertThat(unchangedPoint.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000))
            );
        }

        @DisplayName("다른 사용자의 쿠폰으로 주문 시 실패한다")
        @Test
        void createOrder_withOtherUserCoupon_fails() {
            // given
            String userId = "user123";
            String otherUserId = "user456";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .description("테스트용 쿠폰")
                .build();
            coupon = ((org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>) couponJpaRepository).save(coupon);

            // 다른 사용자의 쿠폰
            UserCoupon otherUserCoupon = UserCoupon.builder()
                .userId(otherUserId)
                .coupon(coupon)
                .build();
            otherUserCoupon = ((org.springframework.data.jpa.repository.JpaRepository<UserCoupon, Long>) userCouponJpaRepository).save(otherUserCoupon);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(50000))
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 1)),
                otherUserCoupon.getId()
            );

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(userId, command)
            );

            assertThat(exception.getMessage()).contains("본인의 쿠폰만 사용할 수 있습니다");
        }

        @DisplayName("재고가 부족하면 주문이 실패하고 트랜잭션이 롤백된다")
        @Test
        void createOrder_withInsufficientStock_failsAndRollback() {
            // given
            String userId = "user123";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(5) // 재고 5개
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .description("테스트용 쿠폰")
                .build();
            coupon = ((org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>) couponJpaRepository).save(coupon);

            UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
            userCoupon = ((org.springframework.data.jpa.repository.JpaRepository<UserCoupon, Long>) userCouponJpaRepository).save(userCoupon);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(50000))
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 10)), // 10개 주문 시도
                userCoupon.getId()
            );

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(userId, command)
            );

            assertThat(exception.getMessage()).contains("재고가 부족합니다");

            // 롤백 확인: 재고, 쿠폰, 포인트 모두 변경되지 않아야 함
            Product unchangedProduct = productRepository.findById(product.getId()).orElseThrow();
            UserCoupon unchangedUserCoupon = userCouponJpaRepository.findById(userCoupon.getId()).orElseThrow();
            Point unchangedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

            assertAll(
                () -> assertThat(unchangedProduct.getStock()).isEqualTo(5),
                () -> assertThat(unchangedUserCoupon.isUsed()).isFalse(),
                () -> assertThat(unchangedPoint.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000))
            );
        }

        @DisplayName("포인트가 부족하면 주문이 실패하고 트랜잭션이 롤백된다")
        @Test
        void createOrder_withInsufficientPoint_failsAndRollback() {
            // given
            String userId = "user123";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Coupon coupon = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .type(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .description("테스트용 쿠폰")
                .build();
            coupon = ((org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>) couponJpaRepository).save(coupon);

            UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
            userCoupon = ((org.springframework.data.jpa.repository.JpaRepository<UserCoupon, Long>) userCouponJpaRepository).save(userCoupon);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(3000)) // 포인트 부족 (10000 - 5000 = 5000 필요, 3000만 보유)
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            OrderCommand.Create command = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 1)),
                userCoupon.getId()
            );

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                orderFacade.createOrder(userId, command)
            );

            assertThat(exception.getMessage()).contains("잔액이 부족합니다");

            // 롤백 확인: 재고, 쿠폰, 포인트 모두 변경되지 않아야 함
            Product unchangedProduct = productRepository.findById(product.getId()).orElseThrow();
            UserCoupon unchangedUserCoupon = userCouponJpaRepository.findById(userCoupon.getId()).orElseThrow();
            Point unchangedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

            assertAll(
                () -> assertThat(unchangedProduct.getStock()).isEqualTo(10),
                () -> assertThat(unchangedUserCoupon.isUsed()).isFalse(),
                () -> assertThat(unchangedPoint.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(3000))
            );
        }
    }

    @DisplayName("주문 취소")
    @Nested
    class CancelOrder {

        @DisplayName("주문을 성공적으로 취소할 수 있다")
        @Test
        @Transactional
        void cancelOrder_success() {
            // given
            String userId = "user123";

            Brand brand = Brand.builder()
                .name("테스트 브랜드")
                .description("테스트용 브랜드")
                .build();
            brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

            Product product = Product.builder()
                .brand(brand)
                .name("테스트 상품")
                .price(BigDecimal.valueOf(10000))
                .stock(10)
                .description("테스트용 상품")
                .build();
            product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

            Point point = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(50000))
                .build();
            point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

            // 주문 생성
            OrderCommand.Create createCommand = new OrderCommand.Create(
                List.of(new OrderItemRequest(product.getId(), 2)),
                null
            );
            OrderInfo orderInfo = orderFacade.createOrder(userId, createCommand);

            // when - 주문 취소
            orderFacade.cancelOrder(orderInfo.id(), userId);

            // then
            Order cancelledOrder = orderRepository.findById(orderInfo.id()).orElseThrow();
            Product restoredProduct = productRepository.findById(product.getId()).orElseThrow();
            Point refundedPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

            assertAll(
                // 주문 상태 확인
                () -> assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED),

                // 재고 복구 확인
                () -> assertThat(restoredProduct.getStock()).isEqualTo(10),

                // 포인트 환불 확인
                () -> assertThat(refundedPoint.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000))
            );
        }
    }
}
