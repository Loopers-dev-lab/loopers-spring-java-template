package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * 쿠폰을 사용한 주문 생성 통합 테스트
 *
 * @author hyunjikoh
 * @since 2025. 11. 20.
 */
@SpringBootTest
@DisplayName("쿠폰을 사용한 주문 생성 통합 테스트")
public class OrderCreateWithCouponIntegrationTest {

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
    @DisplayName("정액 할인 쿠폰 주문")
    class FixedAmountCouponOrder {

        @Test
        @DisplayName("정액 할인 쿠폰을 사용하여 주문을 생성할 수 있다")
        void should_create_order_with_fixed_amount_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명"));

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);
            pointService.charge(userInfo.username(), new BigDecimal("15000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            UserEntity user = userService.getUserByUsername(userInfo.username());

            CouponEntity fixedAmountCoupon = couponService.createFixedAmountCoupon(user, new BigDecimal("5000"));

            // Given: 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(savedProduct.getId())
                                    .quantity(2)
                                    .couponId(fixedAmountCoupon.getId())
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(fixedAmountCoupon.getId(), user.getId());
            assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
        }

        @Test
        @DisplayName("정액 쿠폰 적용 후 총 주문 금액이 정확하게 계산된다")
        void should_calculate_total_amount_correctly_with_fixed_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

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

            // Given: 주문 생성 요청 (수량 2개)
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

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 총 주문 금액 검증
            BigDecimal expectedOriginalAmount = new BigDecimal("20000"); // 10,000 * 2
            BigDecimal expectedDiscountAmount = new BigDecimal("5000");  // 쿠폰 할인
            BigDecimal expectedFinalAmount = new BigDecimal("15000");    // 20,000 - 5,000

            assertThat(result.originalTotalAmount())
                    .as("할인 전 원래 주문 금액")
                    .isEqualByComparingTo(expectedOriginalAmount);

            assertThat(result.discountAmount())
                    .as("쿠폰 할인 금액")
                    .isEqualByComparingTo(expectedDiscountAmount);

            assertThat(result.finalTotalAmount())
                    .as("쿠폰 할인이 적용된 최종 주문 금액")
                    .isEqualByComparingTo(expectedFinalAmount);

            // Then: 주문 항목 금액 검증
            assertThat(result.orderItems()).hasSize(1);
            assertThat(result.orderItems().get(0).quantity()).isEqualTo(2);
            assertThat(result.orderItems().get(0).unitPrice())
                    .as("상품 단가")
                    .isEqualByComparingTo(new BigDecimal("10000"));

            // Then: 실제 할인이 적용되었는지 검증
            BigDecimal itemTotal = result.orderItems().get(0).unitPrice()
                    .multiply(BigDecimal.valueOf(result.orderItems().get(0).quantity()));
            BigDecimal actualDiscount = itemTotal.subtract(result.finalTotalAmount());

            assertThat(actualDiscount)
                    .as("실제 적용된 할인 금액")
                    .isEqualByComparingTo(expectedDiscountAmount);
        }


        @Test
        @DisplayName("정액 쿠폰 할인 후 포인트 차감이 정확하게 이루어진다")
        void should_deduct_points_correctly_after_fixed_coupon_discount() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("30000");
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
            CouponEntity fixedCoupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("3000")
            );

            // Given: 주문 생성 요청 (수량 2개)
            // 예상 계산: (12,000 * 2) - 3,000 = 21,000원
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

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 포인트 차감 검증
            BigDecimal expectedOriginalAmount = new BigDecimal("24000"); // 12,000 * 2
            BigDecimal expectedDiscountAmount = new BigDecimal("3000");  // 쿠폰 할인
            BigDecimal expectedFinalAmount = new BigDecimal("21000");    // 24,000 - 3,000
            BigDecimal expectedRemainingPoints = initialPoints.subtract(expectedFinalAmount); // 30,000 - 21,000 = 9,000

            // Then: 주문 금액 확인
            assertThat(result.finalTotalAmount())
                    .as("쿠폰 할인이 적용된 최종 주문 금액")
                    .isEqualByComparingTo(expectedFinalAmount);

            // Then: 사용자의 남은 포인트 확인
            UserEntity updatedUser = userService.getUserByUsername(userInfo.username());
            assertThat(updatedUser.getPointAmount())
                    .as("주문 후 남은 포인트 (초기 30,000 - 할인 후 금액 21,000)")
                    .isEqualByComparingTo(expectedRemainingPoints);

            // Then: 차감된 포인트가 할인 적용 후 금액과 일치하는지 확인
            BigDecimal deductedPoints = initialPoints.subtract(updatedUser.getPointAmount());
            assertThat(deductedPoints)
                    .as("실제 차감된 포인트는 쿠폰 할인 후 금액과 동일해야 함")
                    .isEqualByComparingTo(expectedFinalAmount);

            // Then: 원래 금액이 아닌 할인된 금액만큼만 차감되었는지 확인
            assertThat(deductedPoints)
                    .as("할인 전 금액(24,000)이 아닌 할인 후 금액(21,000)만 차감")
                    .isLessThan(expectedOriginalAmount)
                    .isEqualByComparingTo(expectedFinalAmount);
        }
    }

    @Nested
    @DisplayName("배율 할인 쿠폰 주문")
    class PercentageCouponOrder {

        @Test
        @DisplayName("배율 할인 쿠폰을 사용하여 주문을 생성할 수 있다")
        void should_create_order_with_percentage_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("20000"));

            // Given: 상품 생성 (단가 10,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 배율 할인 쿠폰 생성 (20% 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity percentageCoupon = couponService.createPercentCoupon(
                    user,
                    20
            );

            // Given: 주문 생성 요청 (수량 2개)
            // 예상 계산: (10,000 * 2) * 0.8 = 16,000원
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

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 주문이 정상적으로 생성되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.orderItems()).hasSize(1);

            // Then: 쿠폰이 사용됨 상태로 변경되었는지 검증
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(percentageCoupon.getId(), user.getId());
            assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
        }

        @Test
        @DisplayName("배율 쿠폰 할인 금액이 소수점 이하 반올림되어 계산된다")
        void should_round_percentage_coupon_discount_correctly() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성 (단가 9,999원 - 소수점 발생하는 금액)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("9999"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 배율 할인 쿠폰 생성 (15% 할인)
            // 9,999 * 0.15 = 1,499.85 -> 반올림 필요
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity percentageCoupon = couponService.createPercentCoupon(
                    user,
                    20
            );

            // Given: 주문 생성 요청 (수량 1개)
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .couponId(percentageCoupon.getId())
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 할인 금액이 정수로 반올림되었는지 검증
            BigDecimal originalAmount = new BigDecimal("9999");
            BigDecimal expectedDiscount = new BigDecimal("2000"); // 9999 * 0.2 = 1999.8 -> 2000 (반올림)
            BigDecimal expectedFinalAmount = originalAmount.subtract(expectedDiscount); // 8,499

            assertThat(result.finalTotalAmount())
                    .as("배율 쿠폰 할인 후 금액 (소수점 반올림 적용)")
                    .isEqualByComparingTo(expectedFinalAmount);

            // Then: 할인 금액에 소수점이 없는지 확인
            assertThat(result.finalTotalAmount().scale())
                    .as("최종 금액은 소수점이 없어야 함")
                    .isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("배율 쿠폰 적용 후 총 주문 금액이 정확하게 계산된다")
        void should_calculate_total_amount_correctly_with_percentage_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("100000");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성 (단가 25,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("25000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 배율 할인 쿠폰 생성 (30% 할인)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity percentageCoupon = couponService.createPercentCoupon(
                    user,
                    30
            );

            // Given: 주문 생성 요청 (수량 3개)
            // 예상 계산: (25,000 * 3) * 0.7 = 52,500원
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(3)
                                    .couponId(percentageCoupon.getId())
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 총 주문 금액 검증
            BigDecimal expectedOriginalAmount = new BigDecimal("75000"); // 25,000 * 3
            BigDecimal expectedDiscountRate = new BigDecimal("0.30");    // 30%
            BigDecimal expectedDiscountAmount = expectedOriginalAmount.multiply(expectedDiscountRate); // 22,500
            BigDecimal expectedFinalAmount = new BigDecimal("52500");    // 75,000 - 22,500

            assertThat(result.finalTotalAmount())
                    .as("배율 쿠폰 할인이 적용된 최종 주문 금액")
                    .isEqualByComparingTo(expectedFinalAmount);

            // Then: 주문 항목 검증
            assertThat(result.orderItems()).hasSize(1);
            assertThat(result.orderItems().get(0).quantity()).isEqualTo(3);
            assertThat(result.orderItems().get(0).unitPrice())
                    .as("상품 단가")
                    .isEqualByComparingTo(new BigDecimal("25000"));

            // Then: 실제 할인율 검증
            BigDecimal itemTotal = result.orderItems().get(0).unitPrice()
                    .multiply(BigDecimal.valueOf(result.orderItems().get(0).quantity()));
            BigDecimal actualDiscount = itemTotal.subtract(result.finalTotalAmount());

            assertThat(actualDiscount)
                    .as("실제 적용된 할인 금액 (30%)")
                    .isEqualByComparingTo(expectedDiscountAmount);

            // Then: 포인트 차감 검증
            UserEntity updatedUser = userService.getUserByUsername(userInfo.username());
            BigDecimal expectedRemainingPoints = initialPoints.subtract(expectedFinalAmount);

            assertThat(updatedUser.getPointAmount())
                    .as("주문 후 남은 포인트")
                    .isEqualByComparingTo(expectedRemainingPoints);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 예외 처리")
    class CouponUsageException {

        @Test
        @DisplayName("이미 사용된 쿠폰으로 주문 시 예외가 발생한다")
        void should_throw_exception_when_using_already_used_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 쿠폰 생성 및 첫 번째 주문으로 사용
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("5000")
            );

            OrderCreateCommand firstOrderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();
            orderFacade.createOrder(firstOrderCommand);

            // Given: 동일한 쿠폰으로 두 번째 주문 시도
            OrderCreateCommand secondOrderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();

            // When & Then: 이미 사용된 쿠폰으로 주문 시 예외 발생
            assertThatThrownBy(() -> orderFacade.createOrder(secondOrderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 사용된 쿠폰입니다.");
        }

        @Test
        @DisplayName("다른 사용자의 쿠폰으로 주문 시 예외가 발생한다")
        void should_throw_exception_when_using_other_users_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 첫 번째 사용자 생성 및 쿠폰 발급
            UserRegisterCommand user1Command = UserTestFixture.createUserCommand(
                    "user1",
                    "user1@example.com",
                    "1990-01-01",
                    Gender.MALE
            );
            UserInfo user1Info = userFacade.registerUser(user1Command);
            UserEntity user1 = userService.getUserByUsername(user1Info.username());
            CouponEntity user1Coupon = couponService.createFixedAmountCoupon(
                    user1,
                    new BigDecimal("5000")
            );

            // Given: 두 번째 사용자 생성 및 포인트 충전
            UserRegisterCommand user2Command = UserTestFixture.createUserCommand(
                    "user2",
                    "user2@example.com",
                    "1990-01-01",
                    Gender.MALE
            );
            UserInfo user2Info = userFacade.registerUser(user2Command);
            pointService.charge(user2Info.username(), new BigDecimal("20000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: user2가 user1의 쿠폰으로 주문 시도
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(user2Info.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .couponId(user1Coupon.getId())
                                    .build()
                    ))
                    .build();

            // When & Then: 다른 사용자의 쿠폰 사용 시 쿠폰 자체를 찾을수 없다.
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("쿠폰을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰 ID로 주문 시 예외가 발생한다")
        void should_throw_exception_when_using_non_existent_coupon() {
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

            // Given: 존재하지 않는 쿠폰 ID로 주문 시도
            Long nonExistentCouponId = 99999L;
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .couponId(nonExistentCouponId)
                                    .build()
                    ))
                    .build();

            // When & Then: 존재하지 않는 쿠폰 ID 사용 시 예외 발생
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("쿠폰을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("쿠폰 할인 후 포인트가 부족하면 주문이 실패한다")
        void should_fail_order_when_insufficient_points_after_coupon_discount() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 부족한 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("5000")); // 부족한 포인트

            // Given: 상품 생성 (단가 10,000원)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 정액 할인 쿠폰 생성 (2,000원 할인)
            // 할인 후 금액: 10,000 - 2,000 = 8,000원 (보유 포인트 5,000원으로 부족)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("2000")
            );

            // Given: 주문 생성 요청
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

            // When & Then: 쿠폰 할인 후에도 포인트 부족으로 주문 실패
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            // Then: 쿠폰은 사용되지 않은 상태로 유지
            CouponEntity unusedCoupon = couponService.getCouponByIdAndUserId(coupon.getId(), user.getId());
            assertThat(unusedCoupon.getStatus()).isEqualTo(CouponStatus.UNUSED);
        }

        @Test
        @DisplayName("쿠폰 할인 후에도 재고가 부족하면 주문이 실패한다")
        void should_fail_order_when_insufficient_stock_even_with_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 재고가 부족한 상품 생성 (재고 5개)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    5 // 재고 5개만
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 쿠폰 생성
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(
                    user,
                    new BigDecimal("5000")
            );

            // Given: 재고보다 많은 수량으로 주문 시도 (10개 주문)
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(10) // 재고(5개)보다 많은 수량
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();

            // When & Then: 쿠폰이 있어도 재고 부족으로 주문 실패
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문할 수 없는 상품입니다.");

            // Then: 쿠폰은 사용되지 않은 상태로 유지
            CouponEntity unusedCoupon = couponService.getCouponByIdAndUserId(coupon.getId(), user.getId());
            assertThat(unusedCoupon.getStatus()).isEqualTo(CouponStatus.UNUSED);

            // Then: 상품 재고는 변경되지 않음
            ProductEntity unchangedProduct = productService.getProductDetail(product.getId());
            assertThat(unchangedProduct.getStockQuantity()).isEqualTo(5);
        }
    }
}
