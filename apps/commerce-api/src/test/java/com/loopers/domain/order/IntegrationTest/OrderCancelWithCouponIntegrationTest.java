package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponStatus;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * 쿠폰을 사용한 주문 취소 통합 테스트
 *
 * @author hyunjikoh
 * @since 2025. 11. 20.
 */
@SpringBootTest
@DisplayName("쿠폰을 사용한 주문 취소 통합 테스트")
public class OrderCancelWithCouponIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private PointService pointService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 필요한 만큼만 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("쿠폰 복구")
    class CouponRestoration {

        @Test
        @DisplayName("정액 쿠폰을 사용한 주문을 취소하면 쿠폰이 복구된다")
        void should_restore_fixed_coupon_when_canceling_order() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("30000");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성 (단가 10,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 정액 할인 쿠폰 생성 (5,000원 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity fixedCoupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("5000")
            );

            // Given: 쿠폰을 사용하여 주문 생성 (수량 2개)
            // 예상 계산: (10,000 * 2) - 5,000 = 15,000원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(2)
                                    .couponId(fixedCoupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 생성 후 쿠폰이 사용됨 상태인지 확인
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(fixedCoupon.getId(), user.getId());
            assertThat(usedCoupon.getStatus())
                    .as("주문 생성 후 쿠폰 상태")
                    .isEqualTo(CouponStatus.USED);

            // When: 주문 취소
            OrderInfo cancelledOrder = orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 쿠폰이 복구되어 UNUSED 상태로 변경되었는지 검증
            CouponEntity restoredCoupon = couponService.getCouponByIdAndUserId(fixedCoupon.getId(), user.getId());
            assertThat(restoredCoupon.getStatus())
                    .as("주문 취소 후 쿠폰 상태가 UNUSED로 복구되어야 함")
                    .isEqualTo(CouponStatus.UNUSED);

            // Then: 쿠폰을 다시 사용할 수 있는지 검증
            assertThat(restoredCoupon.canUse())
                    .as("복구된 쿠폰은 다시 사용 가능해야 함")
                    .isTrue();

            // Then: 포인트가 정확하게 환불되었는지 검증
            UserEntity updatedUser = userService.getUserByUsername(userInfo.username());
            assertThat(updatedUser.getPointAmount())
                    .as("주문 취소 후 포인트가 초기 금액으로 복구되어야 함")
                    .isEqualByComparingTo(initialPoints);

            // Then: 상품 재고가 복구되었는지 검증
            ProductEntity updatedProduct = productService.getProductDetail(product.getId());
            assertThat(updatedProduct.getStockQuantity())
                    .as("주문 취소 후 재고가 복구되어야 함")
                    .isEqualTo(100);
        }

        @Test
        @DisplayName("배율 쿠폰을 사용한 주문을 취소하면 쿠폰이 복구된다")
        void should_restore_percentage_coupon_when_canceling_order() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("50000");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성 (단가 20,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("20000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 배율 할인 쿠폰 생성 (25% 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity percentageCoupon = couponService.createPercentCoupon(
                    user,
                    25
            );

            // Given: 쿠폰을 사용하여 주문 생성 (수량 2개)
            // 예상 계산: (20,000 * 2) * 0.75 = 30,000원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(2)
                                    .couponId(percentageCoupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 생성 후 쿠폰이 사용됨 상태인지 확인
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(percentageCoupon.getId(), user.getId());
            assertThat(usedCoupon.getStatus())
                    .as("주문 생성 후 배율 쿠폰 상태")
                    .isEqualTo(CouponStatus.USED);

            // When: 주문 취소
            orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 배율 쿠폰이 복구되어 UNUSED 상태로 변경되었는지 검증
            CouponEntity restoredCoupon = couponService.getCouponByIdAndUserId(percentageCoupon.getId(), user.getId());
            assertThat(restoredCoupon.getStatus())
                    .as("주문 취소 후 배율 쿠폰 상태가 UNUSED로 복구되어야 함")
                    .isEqualTo(CouponStatus.UNUSED);

            // Then: 쿠폰을 다시 사용할 수 있는지 검증
            assertThat(restoredCoupon.canUse())
                    .as("복구된 배율 쿠폰은 다시 사용 가능해야 함")
                    .isTrue();

            // Then: 포인트가 정확하게 환불되었는지 검증
            UserEntity updatedUser = userService.getUserByUsername(userInfo.username());
            assertThat(updatedUser.getPointAmount())
                    .as("주문 취소 후 포인트가 초기 금액으로 복구되어야 함")
                    .isEqualByComparingTo(initialPoints);
        }

        @Test
        @DisplayName("주문 취소 시 쿠폰 상태가 UNUSED로 변경된다")
        void should_change_coupon_status_to_unused_when_canceling_order() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("20000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 쿠폰 생성
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("3000")
            );

            // Given: 쿠폰 초기 상태 확인
            assertThat(coupon.getStatus())
                    .as("쿠폰 생성 직후 상태")
                    .isEqualTo(CouponStatus.UNUSED);

            // Given: 쿠폰을 사용하여 주문 생성
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 후 쿠폰 상태가 USED로 변경되었는지 확인
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(coupon.getId(), user.getId());
            assertThat(usedCoupon.getStatus())
                    .as("주문 생성 후 쿠폰 상태가 USED로 변경")
                    .isEqualTo(CouponStatus.USED);

            // When: 주문 취소
            orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 쿠폰 상태가 UNUSED로 변경되었는지 검증
            CouponEntity restoredCoupon = couponService.getCouponByIdAndUserId(coupon.getId(), user.getId());
            assertThat(restoredCoupon.getStatus())
                    .as("주문 취소 후 쿠폰 상태가 UNUSED로 복구")
                    .isEqualTo(CouponStatus.UNUSED);

            // Then: 쿠폰 상태 변경 이력 검증 (UNUSED -> USED -> UNUSED)
            assertThat(restoredCoupon.getStatus())
                    .as("최종 쿠폰 상태는 UNUSED")
                    .isNotEqualTo(CouponStatus.USED)
                    .isEqualTo(CouponStatus.UNUSED);
        }

        @Test
        @DisplayName("여러 쿠폰을 사용한 주문 취소 시 모든 쿠폰이 복구된다")
        void should_restore_all_coupons_when_canceling_order_with_multiple_coupons() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("100000");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 2개 생성
            ProductDomainCreateRequest product1Request = ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품1",
                    "상품1 설명",
                    new BigDecimal("15000"),
                    100
            );
            ProductEntity product1 = productService.registerProduct(product1Request);

            ProductDomainCreateRequest product2Request = ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품2",
                    "상품2 설명",
                    new BigDecimal("25000"),
                    100
            );
            ProductEntity product2 = productService.registerProduct(product2Request);

            // Given: 쿠폰 2개 생성 (정액 쿠폰, 배율 쿠폰)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity fixedCoupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("5000")
            );
            CouponEntity percentageCoupon = couponService.createPercentCoupon(
                    user,
                    20
            );

            // Given: 여러 쿠폰을 사용하여 주문 생성
            // 상품1: 15,000 - 5,000 = 10,000원
            // 상품2: 25,000 * 0.8 = 20,000원
            // 총 30,000원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product1.getId())
                                    .quantity(1)
                                    .couponId(fixedCoupon.getId())
                                    .build(),
                            OrderItemCommand.builder()
                                    .productId(product2.getId())
                                    .quantity(1)
                                    .couponId(percentageCoupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 생성 후 모든 쿠폰이 사용됨 상태인지 확인
            CouponEntity usedFixedCoupon = couponService.getCouponByIdAndUserId(fixedCoupon.getId(), user.getId());
            CouponEntity usedPercentageCoupon = couponService.getCouponByIdAndUserId(percentageCoupon.getId(), user.getId());

            assertThat(usedFixedCoupon.getStatus())
                    .as("주문 생성 후 정액 쿠폰 상태")
                    .isEqualTo(CouponStatus.USED);
            assertThat(usedPercentageCoupon.getStatus())
                    .as("주문 생성 후 배율 쿠폰 상태")
                    .isEqualTo(CouponStatus.USED);

            // When: 주문 취소
            orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 모든 쿠폰이 복구되어 UNUSED 상태로 변경되었는지 검증
            CouponEntity restoredFixedCoupon = couponService.getCouponByIdAndUserId(fixedCoupon.getId(), user.getId());
            CouponEntity restoredPercentageCoupon = couponService.getCouponByIdAndUserId(percentageCoupon.getId(), user.getId());

            assertThat(restoredFixedCoupon.getStatus())
                    .as("주문 취소 후 정액 쿠폰이 UNUSED로 복구")
                    .isEqualTo(CouponStatus.UNUSED);
            assertThat(restoredPercentageCoupon.getStatus())
                    .as("주문 취소 후 배율 쿠폰이 UNUSED로 복구")
                    .isEqualTo(CouponStatus.UNUSED);

            // Then: 모든 쿠폰을 다시 사용할 수 있는지 검증
            assertThat(restoredFixedCoupon.canUse())
                    .as("복구된 정액 쿠폰은 다시 사용 가능")
                    .isTrue();
            assertThat(restoredPercentageCoupon.canUse())
                    .as("복구된 배율 쿠폰은 다시 사용 가능")
                    .isTrue();

            // Then: 포인트가 정확하게 환불되었는지 검증
            UserEntity updatedUser = userService.getUserByUsername(userInfo.username());
            assertThat(updatedUser.getPointAmount())
                    .as("주문 취소 후 포인트가 초기 금액으로 복구")
                    .isEqualByComparingTo(initialPoints);

            // Then: 모든 상품의 재고가 복구되었는지 검증
            ProductEntity updatedProduct1 = productService.getProductDetail(product1.getId());
            ProductEntity updatedProduct2 = productService.getProductDetail(product2.getId());

            assertThat(updatedProduct1.getStockQuantity())
                    .as("상품1 재고 복구")
                    .isEqualTo(100);
            assertThat(updatedProduct2.getStockQuantity())
                    .as("상품2 재고 복구")
                    .isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("포인트 및 재고 복구")
    class PointAndStockRestoration {

        @Test
        @DisplayName("쿠폰 사용 주문 취소 시 포인트가 정확하게 환불된다")
        void should_refund_correct_amount_when_canceling_order_with_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("50000");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성 (단가 12,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("12000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 정액 할인 쿠폰 생성 (3,000원 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("3000")
            );

            // Given: 쿠폰을 사용하여 주문 생성 (수량 2개)
            // 원래 금액: 12,000 * 2 = 24,000원
            // 할인 후 금액: 24,000 - 3,000 = 21,000원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(2)
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 후 포인트 확인
            UserEntity userAfterOrder = userService.getUserByUsername(userInfo.username());
            BigDecimal pointsAfterOrder = userAfterOrder.getPointAmount();
            BigDecimal expectedPointsAfterOrder = initialPoints.subtract(new BigDecimal("21000"));

            assertThat(pointsAfterOrder)
                    .as("주문 후 포인트 (50,000 - 21,000 = 29,000)")
                    .isEqualByComparingTo(expectedPointsAfterOrder);

            // When: 주문 취소
            orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 포인트가 정확하게 환불되었는지 검증
            UserEntity userAfterCancel = userService.getUserByUsername(userInfo.username());

            assertThat(userAfterCancel.getPointAmount())
                    .as("주문 취소 후 포인트가 초기 금액으로 복구 (50,000)")
                    .isEqualByComparingTo(initialPoints);

            // Then: 환불된 포인트 금액 검증
            BigDecimal refundedAmount = userAfterCancel.getPointAmount().subtract(pointsAfterOrder);

            assertThat(refundedAmount)
                    .as("환불된 포인트는 실제 결제 금액과 동일 (21,000)")
                    .isEqualByComparingTo(new BigDecimal("21000"));
        }

        @Test
        @DisplayName("쿠폰 사용 주문 취소 시 재고가 정확하게 복구된다")
        void should_restore_stock_correctly_when_canceling_order_with_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 상품 생성 (초기 재고 50개)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("15000"),
                    50
            );
            ProductEntity product = productService.registerProduct(productRequest);
            int initialStock = product.getStockQuantity();

            // Given: 배율 할인 쿠폰 생성 (30% 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createPercentCoupon(
                    user,
                    30
            );

            // Given: 쿠폰을 사용하여 주문 생성 (수량 7개)
            int orderQuantity = 7;
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(orderQuantity)
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 후 재고 확인
            ProductEntity productAfterOrder = productService.getProductDetail(product.getId());
            int expectedStockAfterOrder = initialStock - orderQuantity;

            assertThat(productAfterOrder.getStockQuantity())
                    .as("주문 후 재고 (50 - 7 = 43)")
                    .isEqualTo(expectedStockAfterOrder);

            // When: 주문 취소
            orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 재고가 정확하게 복구되었는지 검증
            ProductEntity productAfterCancel = productService.getProductDetail(product.getId());

            assertThat(productAfterCancel.getStockQuantity())
                    .as("주문 취소 후 재고가 초기 수량으로 복구 (50)")
                    .isEqualTo(initialStock);

            // Then: 복구된 재고 수량 검증
            int restoredStock = productAfterCancel.getStockQuantity() - productAfterOrder.getStockQuantity();

            assertThat(restoredStock)
                    .as("복구된 재고는 주문 수량과 동일 (7)")
                    .isEqualTo(orderQuantity);
        }

        @Test
        @DisplayName("쿠폰 할인 금액을 제외한 실제 결제 금액만 환불된다")
        void should_refund_only_actual_payment_amount_excluding_coupon_discount() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("80000");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성 (단가 30,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("30000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 정액 할인 쿠폰 생성 (10,000원 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("10000")
            );

            // Given: 쿠폰을 사용하여 주문 생성 (수량 2개)
            // 원래 금액: 30,000 * 2 = 60,000원
            // 쿠폰 할인: -10,000원
            // 실제 결제 금액: 50,000원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(2)
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 후 포인트 확인
            UserEntity userAfterOrder = userService.getUserByUsername(userInfo.username());
            BigDecimal actualPaymentAmount = new BigDecimal("50000");
            BigDecimal expectedPointsAfterOrder = initialPoints.subtract(actualPaymentAmount);

            assertThat(userAfterOrder.getPointAmount())
                    .as("주문 후 포인트 (80,000 - 50,000 = 30,000)")
                    .isEqualByComparingTo(expectedPointsAfterOrder);

            // When: 주문 취소
            orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 실제 결제 금액만 환불되었는지 검증
            UserEntity userAfterCancel = userService.getUserByUsername(userInfo.username());
            BigDecimal refundedAmount = userAfterCancel.getPointAmount().subtract(userAfterOrder.getPointAmount());

            assertThat(refundedAmount)
                    .as("환불된 금액은 실제 결제 금액 (50,000원)이어야 함")
                    .isEqualByComparingTo(actualPaymentAmount);

            // Then: 쿠폰 할인 금액은 환불되지 않음을 검증
            BigDecimal originalAmount = new BigDecimal("60000");
            BigDecimal couponDiscount = new BigDecimal("10000");

            assertThat(refundedAmount)
                    .as("환불 금액은 원래 금액(60,000)이 아닌 할인 후 금액(50,000)")
                    .isNotEqualByComparingTo(originalAmount)
                    .isEqualByComparingTo(originalAmount.subtract(couponDiscount));

            // Then: 최종 포인트가 초기 금액으로 복구되었는지 검증
            assertThat(userAfterCancel.getPointAmount())
                    .as("주문 취소 후 포인트가 초기 금액으로 복구 (80,000)")
                    .isEqualByComparingTo(initialPoints);
        }

        @Test
        @DisplayName("여러 쿠폰 사용 주문 취소 시 포인트와 재고가 모두 정확하게 복구된다")
        void should_restore_points_and_stock_correctly_with_multiple_coupons() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("150000");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 3개 생성
            ProductDomainCreateRequest product1Request = ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품1",
                    "상품1 설명",
                    new BigDecimal("20000"),
                    80
            );
            ProductEntity product1 = productService.registerProduct(product1Request);
            int product1InitialStock = product1.getStockQuantity();

            ProductDomainCreateRequest product2Request = ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품2",
                    "상품2 설명",
                    new BigDecimal("30000"),
                    60
            );
            ProductEntity product2 = productService.registerProduct(product2Request);
            int product2InitialStock = product2.getStockQuantity();

            ProductDomainCreateRequest product3Request = ProductTestFixture.createRequest(
                    brand.getId(),
                    "상품3",
                    "상품3 설명",
                    new BigDecimal("40000"),
                    50
            );
            ProductEntity product3 = productService.registerProduct(product3Request);
            int product3InitialStock = product3.getStockQuantity();

            // Given: 쿠폰 3개 생성
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity fixedCoupon1 = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("5000")
            );
            CouponEntity percentageCoupon = couponService.createPercentCoupon(
                    user,
                    20
            );
            CouponEntity fixedCoupon2 = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("8000")
            );

            // Given: 여러 쿠폰을 사용하여 주문 생성
            // 상품1: (20,000 * 2) - 5,000 = 35,000원
            // 상품2: (30,000 * 1) * 0.8 = 24,000원
            // 상품3: (40,000 * 1) - 8,000 = 32,000원
            // 총 결제 금액: 91,000원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product1.getId())
                                    .quantity(2)
                                    .couponId(fixedCoupon1.getId())
                                    .build(),
                            OrderItemCommand.builder()
                                    .productId(product2.getId())
                                    .quantity(1)
                                    .couponId(percentageCoupon.getId())
                                    .build(),
                            OrderItemCommand.builder()
                                    .productId(product3.getId())
                                    .quantity(1)
                                    .couponId(fixedCoupon2.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 후 포인트와 재고 확인
            UserEntity userAfterOrder = userService.getUserByUsername(userInfo.username());
            BigDecimal totalPaymentAmount = new BigDecimal("91000");

            assertThat(userAfterOrder.getPointAmount())
                    .as("주문 후 포인트 (150,000 - 91,000 = 59,000)")
                    .isEqualByComparingTo(initialPoints.subtract(totalPaymentAmount));

            ProductEntity product1AfterOrder = productService.getProductDetail(product1.getId());
            ProductEntity product2AfterOrder = productService.getProductDetail(product2.getId());
            ProductEntity product3AfterOrder = productService.getProductDetail(product3.getId());

            assertThat(product1AfterOrder.getStockQuantity()).isEqualTo(product1InitialStock - 2);
            assertThat(product2AfterOrder.getStockQuantity()).isEqualTo(product2InitialStock - 1);
            assertThat(product3AfterOrder.getStockQuantity()).isEqualTo(product3InitialStock - 1);

            // When: 주문 취소
            orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 포인트가 정확하게 환불되었는지 검증
            UserEntity userAfterCancel = userService.getUserByUsername(userInfo.username());

            assertThat(userAfterCancel.getPointAmount())
                    .as("주문 취소 후 포인트가 초기 금액으로 복구 (150,000)")
                    .isEqualByComparingTo(initialPoints);

            // Then: 모든 상품의 재고가 정확하게 복구되었는지 검증
            ProductEntity product1AfterCancel = productService.getProductDetail(product1.getId());
            ProductEntity product2AfterCancel = productService.getProductDetail(product2.getId());
            ProductEntity product3AfterCancel = productService.getProductDetail(product3.getId());

            assertThat(product1AfterCancel.getStockQuantity())
                    .as("상품1 재고 복구 (80)")
                    .isEqualTo(product1InitialStock);
            assertThat(product2AfterCancel.getStockQuantity())
                    .as("상품2 재고 복구 (60)")
                    .isEqualTo(product2InitialStock);
            assertThat(product3AfterCancel.getStockQuantity())
                    .as("상품3 재고 복구 (50)")
                    .isEqualTo(product3InitialStock);

            // Then: 환불된 포인트 금액 검증
            BigDecimal refundedAmount = userAfterCancel.getPointAmount().subtract(userAfterOrder.getPointAmount());

            assertThat(refundedAmount)
                    .as("환불된 포인트는 총 결제 금액과 동일 (91,000)")
                    .isEqualByComparingTo(totalPaymentAmount);
        }
    }
}
