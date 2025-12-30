package com.loopers.domain.order;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Service 테스트")
@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long testUserId;
    private Product testProduct;
    private Order testOrder;

    private final String testLoginId = "test34";

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId(testLoginId)
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);

        // 생성된 User의 ID를 가져옴
        testUserId = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"))
                .getId();

        // 테스트용 Brand 생성
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

        // 테스트용 Product 생성
        testProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(10000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(savedBrand.getId())
                .build();
        testProduct = productRepository.save(testProduct)
                .orElseThrow(() -> new RuntimeException("Product 저장 실패"));

        // 테스트용 Order 생성
        testOrder = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .userId(testUserId)
                .build();
        testOrder.addOrderItem(testProduct.getId(), testProduct.getName(), testProduct.getPrice(), 2);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("saveOrder 테스트")
    @Nested
    class SaveOrderTest {

        @DisplayName("성공 케이스: 주문 저장 성공")
        @Test
        void saveOrder_withValidOrder_Success() {
            // arrange
            Order order = Order.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .userId(testUserId)
                    .build();
            order.addOrderItem(testProduct.getId(), testProduct.getName(), testProduct.getPrice(), 2);

            // act
            Order savedOrder = orderService.saveOrder(order);

            // assert
            assertNotNull(savedOrder);
            assertNotNull(savedOrder.getId(), "주문 ID는 null이 아니어야 함");
            assertEquals(OrderStatus.PENDING, savedOrder.getOrderStatus(), "주문 상태는 PENDING이어야 함");
            assertEquals(testUserId, savedOrder.getUserId(), "사용자 ID가 일치해야 함");
            assertEquals(1, savedOrder.getOrderItems().size(), "주문 상품 수가 일치해야 함");
        }
    }

    @DisplayName("findOrderById 테스트")
    @Nested
    class FindOrderByIdTest {

        @DisplayName("성공 케이스: 존재하는 주문 조회 성공")
        @Test
        void findOrderById_withExistingOrder_Success() {
            // arrange
            Order savedOrder = orderService.saveOrder(testOrder);

            // act
            Order foundOrder = orderService.findOrderById(savedOrder.getId());

            // assert
            assertNotNull(foundOrder);
            assertEquals(savedOrder.getId(), foundOrder.getId(), "주문 ID가 일치해야 함");
            assertEquals(savedOrder.getOrderStatus(), foundOrder.getOrderStatus(), "주문 상태가 일치해야 함");
            assertEquals(savedOrder.getUserId(), foundOrder.getUserId(), "사용자 ID가 일치해야 함");
        }

        @DisplayName("실패 케이스: 존재하지 않는 주문 조회 시 NOT_FOUND 예외 발생")
        @Test
        void findOrderById_withNonExistentOrder_NotFound() {
            // arrange
            Long nonExistentOrderId = 99999L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    orderService.findOrderById(nonExistentOrderId)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType(),
                    String.format("예상 ErrorType: NOT_FOUND, 실제 ErrorType: %s", exception.getErrorType()));
            assertTrue(exception.getCustomMessage().contains("[orderId = 99999] Order를 찾을 수 없습니다"),
                    String.format("예상 메시지: '[orderId = 99999] Order를 찾을 수 없습니다' 포함, 실제 메시지: %s", exception.getCustomMessage()));
        }
    }

    @DisplayName("saveSuccessOrder 테스트")
    @Nested
    class SaveSuccessOrderTest {

        @DisplayName("성공 케이스: 주문 상태 CONFIRMED로 변경 성공")
        @Test
        void saveSuccessOrder_withPendingOrder_Success() {
            // arrange
            Order savedOrder = orderService.saveOrder(testOrder);
            assertEquals(OrderStatus.PENDING, savedOrder.getOrderStatus(), "초기 주문 상태는 PENDING이어야 함");

            // act
            Order confirmedOrder = orderService.saveSuccessOrder(savedOrder.getId(), LocalDateTime.now());

            // assert
            assertNotNull(confirmedOrder);
            assertEquals(savedOrder.getId(), confirmedOrder.getId(), "주문 ID가 일치해야 함");
            assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getOrderStatus(), "주문 상태는 CONFIRMED여야 함");

            // 다시 조회해서 확인
            Order foundOrder = orderService.findOrderById(savedOrder.getId());
            assertEquals(OrderStatus.CONFIRMED, foundOrder.getOrderStatus(), "DB에 저장된 주문 상태도 CONFIRMED여야 함");
        }
    }

    @DisplayName("saveFailedOrder 테스트")
    @Nested
    class SaveFailedOrderTest {

        @DisplayName("성공 케이스: 주문 상태 FAILED로 변경 및 실패 사유 저장 성공")
        @Test
        void saveFailedOrder_withPendingOrder_Success() {
            // arrange
            Order savedOrder = orderService.saveOrder(testOrder);
            assertEquals(OrderStatus.PENDING, savedOrder.getOrderStatus(), "초기 주문 상태는 PENDING이어야 함");
            String errorMessage = "재고 부족";

            // act
            Order failedOrder = orderService.saveFailedOrder(savedOrder.getId(), errorMessage, LocalDateTime.now());

            // assert
            assertNotNull(failedOrder);
            assertEquals(savedOrder.getId(), failedOrder.getId(), "주문 ID가 일치해야 함");
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getOrderStatus(), "주문 상태는 PAYMENT_FAILED여야 함");

            // 다시 조회해서 확인
            Order foundOrder = orderService.findOrderById(savedOrder.getId());
            assertEquals(OrderStatus.PAYMENT_FAILED, foundOrder.getOrderStatus(), "DB에 저장된 주문 상태도 PAYMENT_FAILED여야 함");
            assertEquals(errorMessage, foundOrder.getErrorMessage(), "실패 사유가 저장되어야 함");
        }

        @DisplayName("실패 케이스: 존재하지 않는 주문에 대해 실패 처리 시도 시 NOT_FOUND 예외 발생")
        @Test
        void saveFailedOrder_withNonExistentOrder_NotFound() {
            // arrange
            Long nonExistentOrderId = 99999L;
            String errorMessage = "재고 부족";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    orderService.saveFailedOrder(nonExistentOrderId, errorMessage, LocalDateTime.now())
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType(),
                    String.format("예상 ErrorType: NOT_FOUND, 실제 ErrorType: %s", exception.getErrorType()));
        }
    }
}
