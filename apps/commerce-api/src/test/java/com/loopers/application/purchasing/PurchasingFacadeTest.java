package com.loopers.application.purchasing;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.Point;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("PurchasingFacade 주문 스펙 검증")
class PurchasingFacadeTest {

    @Autowired
    private PurchasingFacade purchasingFacade;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;
    
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    // Helper methods for test fixtures
    private User createAndSaveUser(String userId, String email, long point) {
        User user = User.of(userId, email, "1990-01-01", Gender.MALE, Point.of(point));
        return userRepository.save(user);
    }

    private Brand createAndSaveBrand(String brandName) {
        Brand brand = Brand.of(brandName);
        return brandRepository.save(brand);
    }

    private Product createAndSaveProduct(String productName, int price, int stock, Long brandId) {
        Product product = Product.of(productName, price, stock, brandId);
        return productRepository.save(product);
    }

    /**
     * 쿠폰을 생성하고 저장합니다.
     *
     * @param code 쿠폰 코드
     * @param type 쿠폰 타입
     * @param discountValue 할인 값
     * @return 저장된 쿠폰
     */
    private Coupon createAndSaveCoupon(String code, CouponType type, Integer discountValue) {
        Coupon coupon = Coupon.of(code, type, discountValue);
        return couponRepository.save(coupon);
    }

    /**
     * 사용자 쿠폰을 생성하고 저장합니다.
     * <p>
     * 쿠폰은 이미 저장된 상태여야 합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param coupon 저장된 쿠폰
     * @return 저장된 사용자 쿠폰
     */
    private UserCoupon createAndSaveUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = UserCoupon.of(userId, coupon);
        return userCouponRepository.save(userCoupon);
    }

    @Test
    @DisplayName("주문 생성 시 재고 차감, 포인트 차감, 주문 완료, 외부 전송을 수행한다")
    void createOrder_successFlow() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 50_000L);
        Brand brand = createAndSaveBrand("브랜드");
        Product product1 = createAndSaveProduct("상품1", 10_000, 10, brand.getId());
        Product product2 = createAndSaveProduct("상품2", 5_000, 5, brand.getId());

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(product1.getId(), 2),
            OrderItemCommand.of(product2.getId(), 1)
        );

        // act
        OrderInfo orderInfo = purchasingFacade.createOrder(user.getUserId(), commands);

        // assert
        assertThat(orderInfo.status()).isEqualTo(OrderStatus.COMPLETED);
        
        // 재고 차감 확인
        Product savedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product savedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
        assertThat(savedProduct1.getStock()).isEqualTo(8); // 10 - 2
        assertThat(savedProduct2.getStock()).isEqualTo(4); // 5 - 1
        
        // 포인트 차감 확인
        User savedUser = userRepository.findByUserId(user.getUserId());
        assertThat(savedUser.getPoint().getValue()).isEqualTo(25_000L); // 50_000 - (10_000 * 2 + 5_000 * 1)
    }

    @Test
    @DisplayName("주문 아이템이 비어 있으면 예외를 던진다")
    void createOrder_emptyItems_throwsException() {
        // arrange
        String userId = "user";
        List<OrderItemCommand> emptyCommands = List.of();

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, emptyCommands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 예외를 던진다")
    void createOrder_userNotFound() {
        // arrange
        String unknownUserId = "unknown";
        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(1L, 1)
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(unknownUserId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("상품 재고가 부족하면 예외를 던지고 포인트는 차감되지 않는다")
    void createOrder_stockNotEnough() {
        // arrange
        User user = createAndSaveUser("testuser2", "test2@example.com", 50_000L);
        final String userId = user.getUserId();

        Brand brand = createAndSaveBrand("브랜드2");
        Product product = createAndSaveProduct("상품", 10_000, 1, brand.getId());
        final Long productId = product.getId();
        final int initialStock = product.getStock();

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(productId, 2)
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST);

        // 롤백 확인: 포인트가 차감되지 않았는지 확인
        User savedUser = userRepository.findByUserId(userId);
        assertThat(savedUser.getPoint().getValue()).isEqualTo(50_000L);
        
        // 롤백 확인: 재고가 변경되지 않았는지 확인
        Product savedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(savedProduct.getStock()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("상품 재고가 0이면 예외를 던지고 포인트는 차감되지 않는다")
    void createOrder_stockZero() {
        // arrange
        User user = createAndSaveUser("testuser2", "test2@example.com", 50_000L);
        final String userId = user.getUserId();

        Brand brand = createAndSaveBrand("브랜드2");
        Product product = createAndSaveProduct("상품", 10_000, 0, brand.getId());
        final Long productId = product.getId();
        final int initialStock = product.getStock();

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(productId, 1)
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST);

        // 롤백 확인: 포인트가 차감되지 않았는지 확인
        User savedUser = userRepository.findByUserId(userId);
        assertThat(savedUser.getPoint().getValue()).isEqualTo(50_000L);
        
        // 롤백 확인: 재고가 변경되지 않았는지 확인
        Product savedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(savedProduct.getStock()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("유저의 포인트 잔액이 부족하면 예외를 던지고 재고는 차감되지 않는다")
    void createOrder_pointNotEnough() {
        // arrange
        User user = createAndSaveUser("testuser2", "test2@example.com", 5_000L);
        final String userId = user.getUserId();

        Brand brand = createAndSaveBrand("브랜드2");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());
        final Long productId = product.getId();
        final int initialStock = product.getStock();

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(productId, 1)
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST);

        // 롤백 확인: 포인트가 차감되지 않았는지 확인
        User savedUser = userRepository.findByUserId(userId);
        assertThat(savedUser.getPoint().getValue()).isEqualTo(5_000L);
        
        // 롤백 확인: 재고가 변경되지 않았는지 확인
        Product savedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(savedProduct.getStock()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("동일 상품을 중복 주문하면 예외를 던진다")
    void createOrder_duplicateProducts_throwsException() {
        // arrange
        User user = createAndSaveUser("testuser3", "test3@example.com", 50_000L);
        final String userId = user.getUserId();

        Brand brand = createAndSaveBrand("브랜드3");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());
        final Long productId = product.getId();

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(productId, 1),
            OrderItemCommand.of(productId, 2)
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST);

        // 주문이 저장되지 않았는지 확인
        List<OrderInfo> orders = purchasingFacade.getOrders(userId);
        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("사용자의 주문 목록을 조회한다")
    void getOrders_returnsUserOrders() {
        // arrange
        User user = createAndSaveUser("testuser4", "test4@example.com", 50_000L);
        Brand brand = createAndSaveBrand("브랜드4");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(product.getId(), 1)
        );
        purchasingFacade.createOrder(user.getUserId(), commands);

        // act
        List<OrderInfo> orders = purchasingFacade.getOrders(user.getUserId());

        // assert
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("사용자의 단일 주문을 조회한다")
    void getOrder_returnsSingleOrder() {
        // arrange
        User user = createAndSaveUser("testuser5", "test5@example.com", 50_000L);
        Brand brand = createAndSaveBrand("브랜드5");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(product.getId(), 1)
        );
        OrderInfo createdOrder = purchasingFacade.createOrder(user.getUserId(), commands);

        // act
        OrderInfo found = purchasingFacade.getOrder(user.getUserId(), createdOrder.orderId());

        // assert
        assertThat(found.orderId()).isEqualTo(createdOrder.orderId());
        assertThat(found.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("다른 사용자의 주문은 조회할 수 없다")
    void getOrder_withDifferentUser_throwsException() {
        // arrange
        User user1 = createAndSaveUser("user1", "user1@example.com", 50_000L);
        User user2 = createAndSaveUser("user2", "user2@example.com", 50_000L);
        final String user1Id = user1.getUserId();
        final String user2Id = user2.getUserId();

        Brand brand = createAndSaveBrand("브랜드6");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(product.getId(), 1)
        );
        OrderInfo user1Order = purchasingFacade.createOrder(user1Id, commands);
        final Long orderId = user1Order.orderId();

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.getOrder(user2Id, orderId))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("주문 전체 흐름에 대해 원자성이 보장되어야 한다 - 실패 시 모든 작업이 롤백된다")
    void createOrder_atomicityGuaranteed() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 50_000L);
        final String userId = user.getUserId();
        final long initialPoint = user.getPoint().getValue();

        Brand brand = createAndSaveBrand("브랜드");
        Product product1 = createAndSaveProduct("상품1", 10_000, 5, brand.getId());
        Product product2 = createAndSaveProduct("상품2", 20_000, 3, brand.getId());
        final Long product1Id = product1.getId();
        final Long product2Id = product2.getId();
        final int initialStock1 = product1.getStock();
        final int initialStock2 = product2.getStock();

        // product2의 재고가 부족한 상황 (3개 재고인데 5개 주문)
        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(product1Id, 2),
            OrderItemCommand.of(product2Id, 5) // 재고 부족
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST);

        // 롤백 확인: 포인트가 차감되지 않았는지 확인
        User savedUser = userRepository.findByUserId(userId);
        assertThat(savedUser.getPoint().getValue()).isEqualTo(initialPoint);
        
        // 롤백 확인: 모든 상품의 재고가 변경되지 않았는지 확인
        Product savedProduct1 = productRepository.findById(product1Id).orElseThrow();
        Product savedProduct2 = productRepository.findById(product2Id).orElseThrow();
        assertThat(savedProduct1.getStock()).isEqualTo(initialStock1);
        assertThat(savedProduct2.getStock()).isEqualTo(initialStock2);
        
        // 롤백 확인: 주문이 저장되지 않았는지 확인
        List<OrderInfo> orders = purchasingFacade.getOrders(userId);
        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("주문 성공 시, 모든 처리는 정상 반영되어야 한다 - 재고, 포인트, 주문 모두 반영")
    void createOrder_success_allOperationsReflected() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 100_000L);
        final String userId = user.getUserId();
        final long initialPoint = user.getPoint().getValue();

        Brand brand = createAndSaveBrand("브랜드");
        Product product1 = createAndSaveProduct("상품1", 10_000, 10, brand.getId());
        Product product2 = createAndSaveProduct("상품2", 15_000, 5, brand.getId());
        final Long product1Id = product1.getId();
        final Long product2Id = product2.getId();
        final int initialStock1 = product1.getStock();
        final int initialStock2 = product2.getStock();

        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(product1Id, 3),
            OrderItemCommand.of(product2Id, 2)
        );
        final int totalAmount = (10_000 * 3) + (15_000 * 2);

        // act
        OrderInfo orderInfo = purchasingFacade.createOrder(userId, commands);

        // assert
        // 주문이 정상적으로 생성되었는지 확인
        assertThat(orderInfo.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(orderInfo.items()).hasSize(2);
        
        // 재고가 정상적으로 차감되었는지 확인
        Product savedProduct1 = productRepository.findById(product1Id).orElseThrow();
        Product savedProduct2 = productRepository.findById(product2Id).orElseThrow();
        assertThat(savedProduct1.getStock()).isEqualTo(initialStock1 - 3);
        assertThat(savedProduct2.getStock()).isEqualTo(initialStock2 - 2);
        
        // 포인트가 정상적으로 차감되었는지 확인
        User savedUser = userRepository.findByUserId(userId);
        assertThat(savedUser.getPoint().getValue()).isEqualTo(initialPoint - totalAmount);
        
        // 주문이 저장되었는지 확인
        List<OrderInfo> orders = purchasingFacade.getOrders(userId);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).orderId()).isEqualTo(orderInfo.orderId());
    }

    @Test
    @DisplayName("정액 쿠폰을 적용하여 주문할 수 있다")
    void createOrder_withFixedAmountCoupon_success() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 50_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("브랜드");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        Coupon coupon = createAndSaveCoupon("FIXED5000", CouponType.FIXED_AMOUNT, 5_000);
        createAndSaveUserCoupon(user.getId(), coupon);

        List<OrderItemCommand> commands = List.of(
            new OrderItemCommand(product.getId(), 1, "FIXED5000")
        );

        // act
        OrderInfo orderInfo = purchasingFacade.createOrder(userId, commands);

        // assert
        assertThat(orderInfo.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(orderInfo.totalAmount()).isEqualTo(5_000); // 10,000 - 5,000 = 5,000

        // 쿠폰이 사용되었는지 확인
        UserCoupon savedUserCoupon = userCouponRepository.findByUserIdAndCouponCode(user.getId(), "FIXED5000")
            .orElseThrow();
        assertThat(savedUserCoupon.getIsUsed()).isTrue();
    }

    @Test
    @DisplayName("정률 쿠폰을 적용하여 주문할 수 있다")
    void createOrder_withPercentageCoupon_success() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 50_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("브랜드");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        Coupon coupon = createAndSaveCoupon("PERCENT20", CouponType.PERCENTAGE, 20);
        createAndSaveUserCoupon(user.getId(), coupon);

        List<OrderItemCommand> commands = List.of(
            new OrderItemCommand(product.getId(), 1, "PERCENT20")
        );

        // act
        OrderInfo orderInfo = purchasingFacade.createOrder(userId, commands);

        // assert
        assertThat(orderInfo.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(orderInfo.totalAmount()).isEqualTo(8_000); // 10,000 - (10,000 * 20%) = 8,000

        // 쿠폰이 사용되었는지 확인
        UserCoupon savedUserCoupon = userCouponRepository.findByUserIdAndCouponCode(user.getId(), "PERCENT20")
            .orElseThrow();
        assertThat(savedUserCoupon.getIsUsed()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰으로 주문하면 실패한다")
    void createOrder_withNonExistentCoupon_shouldFail() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 50_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("브랜드");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        List<OrderItemCommand> commands = List.of(
            new OrderItemCommand(product.getId(), 1, "NON_EXISTENT")
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("사용자가 소유하지 않은 쿠폰으로 주문하면 실패한다")
    void createOrder_withCouponNotOwnedByUser_shouldFail() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 50_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("브랜드");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        Coupon coupon = Coupon.of("COUPON001", CouponType.FIXED_AMOUNT, 5_000);
        couponRepository.save(coupon);
        // 사용자에게 쿠폰을 지급하지 않음

        List<OrderItemCommand> commands = List.of(
            new OrderItemCommand(product.getId(), 1, "COUPON001")
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("이미 사용된 쿠폰으로 주문하면 실패한다")
    void createOrder_withUsedCoupon_shouldFail() {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 50_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("브랜드");
        Product product = createAndSaveProduct("상품", 10_000, 10, brand.getId());

        Coupon coupon = createAndSaveCoupon("USED_COUPON", CouponType.FIXED_AMOUNT, 5_000);
        UserCoupon userCoupon = createAndSaveUserCoupon(user.getId(), coupon);
        userCoupon.use(); // 이미 사용 처리
        userCouponRepository.save(userCoupon);

        List<OrderItemCommand> commands = List.of(
            new OrderItemCommand(product.getId(), 1, "USED_COUPON")
        );

        // act & assert
        assertThatThrownBy(() -> purchasingFacade.createOrder(userId, commands))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST);
    }

}
