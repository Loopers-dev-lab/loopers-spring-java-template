package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order 테스트")
public class OrderTest {

    // 고정 fixture
    private final BigDecimal validDiscountAmount = BigDecimal.valueOf(1000);
    private final BigDecimal validShippingFee = BigDecimal.valueOf(2000);

    private User createValidUser() {
        return User.builder()
                .loginId("testuser1")
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
    }

    private Product createValidProduct() {
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();

        // Product 생성 시 필드 초기화로 Stock이 자동 생성됨 (quantity=0L)
        // Product.builder()로 생성하면 stock 필드가 자동으로 초기화됨
        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(5000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brand(brand)
                .build();

        return product;
    }


    @DisplayName("Order 엔티티 생성")
    @Nested
    class CreateOrderTest {

        @DisplayName("성공 케이스 : 필드가 모두 형식에 맞으면 Order 객체 생성 성공")
        @Test
        void createOrder_withValidFields_Success() {
            // arrange
            User user = createValidUser();
            Product product = createValidProduct();
            
            // act
            Order order = Order.builder()
                    .discountAmount(validDiscountAmount)
                    .shippingFee(validShippingFee)
                    .user(user)
                    .build();
            
            // OrderItem 추가
            order.addOrderItem(product, 2);
            
            // 생성 시 자동 계산되는 값들
            // product.getPrice() * quantity = 5000 * 2 = 10000
            BigDecimal expectedTotalPrice = product.getPrice().multiply(BigDecimal.valueOf(2));
            BigDecimal expectedFinalAmount = expectedTotalPrice
                    .subtract(validDiscountAmount)
                    .add(validShippingFee); // totalPrice - discountAmount + shippingFee

            // assert
            assertNotNull(order);
            assertAll(
                    () -> assertEquals(validDiscountAmount, order.getDiscountAmount()),
                    () -> assertEquals(validShippingFee, order.getShippingFee()),
                    () -> assertEquals(OrderStatus.PENDING, order.getOrderStatus(), "orderStatus는 기본값 PENDING이어야 함"),
                    () -> assertEquals(user, order.getUser()),
                    () -> assertEquals(1, order.getOrderItems().size()),
                    // 자동 계산 검증
                    () -> assertEquals(expectedTotalPrice, order.getTotalPrice(), "totalPrice는 orderItems의 totalAmount 합계로 자동 계산되어야 함"),
                    () -> assertEquals(expectedFinalAmount, order.getFinalAmount(), "finalAmount는 totalPrice - discountAmount + shippingFee로 자동 계산되어야 함"),
                    // 양방향 관계 설정 검증
                    () -> assertEquals(order, order.getOrderItems().get(0).getOrder(), "각 OrderItem의 order가 Order를 참조해야 함 (양방향 관계)")
            );
        }


        @DisplayName("실패 케이스 : discountAmount가 null이면 예외 발생")
        @Test
        void createOrder_withNullDiscountAmount_ThrowsException() {
            // arrange
            User user = createValidUser();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Order.builder()
                            .discountAmount(null)
                            .shippingFee(validShippingFee)
                            .user(user)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("discountAmount가 비어있을 수 없습니다"));
        }

        @DisplayName("실패 케이스 : discountAmount가 음수이면 예외 발생")
        @Test
        void createOrder_withNegativeDiscountAmount_ThrowsException() {
            // arrange
            User user = createValidUser();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Order.builder()
                            .discountAmount(BigDecimal.valueOf(-1000))
                            .shippingFee(validShippingFee)
                            .user(user)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("discountAmount는 음수가 될 수 없습니다"));
        }

        @DisplayName("실패 케이스 : shippingFee가 null이면 예외 발생")
        @Test
        void createOrder_withNullShippingFee_ThrowsException() {
            // arrange
            User user = createValidUser();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Order.builder()
                            .discountAmount(validDiscountAmount)
                            .shippingFee(null)
                            .user(user)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("shippingFee가 비어있을 수 없습니다"));
        }

        @DisplayName("실패 케이스 : shippingFee가 음수이면 예외 발생")
        @Test
        void createOrder_withNegativeShippingFee_ThrowsException() {
            // arrange
            User user = createValidUser();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Order.builder()
                            .discountAmount(validDiscountAmount)
                            .shippingFee(BigDecimal.valueOf(-1000))
                            .user(user)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("shippingFee는 음수가 될 수 없습니다"));
        }

        @DisplayName("실패 케이스 : user가 null이면 예외 발생")
        @Test
        void createOrder_withNullUser_ThrowsException() {
            // arrange

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Order.builder()
                            .discountAmount(validDiscountAmount)
                            .shippingFee(validShippingFee)
                            .user(null)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("user가 비어있을 수 없습니다"));
        }

    }

    @DisplayName("Order addOrderItem 메서드")
    @Nested
    class AddOrderItemTest {

        @DisplayName("성공 케이스 : PENDING 상태일 때 OrderItem 추가 성공")
        @Test
        void addOrderItem_whenStatusIsPending_Success() {
            // arrange
            User user = createValidUser();
            Product product = createValidProduct();
            
            Order order = Order.builder()
                    .discountAmount(validDiscountAmount)
                    .shippingFee(validShippingFee)
                    .user(user)
                    .build();
            
            // 첫 번째 OrderItem 추가
            order.addOrderItem(product, 2);
            BigDecimal initialTotalPrice = order.getTotalPrice();
            
            // 두 번째 OrderItem 추가를 위한 예상 값 계산
            BigDecimal newItemTotalAmount = product.getPrice().multiply(BigDecimal.valueOf(1));
            BigDecimal expectedTotalPrice = initialTotalPrice.add(newItemTotalAmount);
            BigDecimal expectedFinalAmount = expectedTotalPrice
                    .subtract(validDiscountAmount)
                    .add(validShippingFee);

            // act
            order.addOrderItem(product, 1);

            // assert
            assertAll(
                    () -> assertEquals(2, order.getOrderItems().size(), "OrderItem이 추가되어야 함"),
                    () -> assertEquals(expectedTotalPrice, order.getTotalPrice(), "totalPrice는 기존 값 + 새 OrderItem의 totalAmount여야 함"),
                    () -> assertEquals(expectedFinalAmount, order.getFinalAmount(), "finalAmount는 totalPrice - discountAmount + shippingFee로 재계산되어야 함")
            );
        }

        @DisplayName("실패 케이스 : PENDING 상태가 아닐 때 OrderItem 추가하면 예외 발생")
        @Test
        void addOrderItem_whenStatusIsNotPending_ThrowsException() {
            // arrange
            User user = createValidUser();
            Product product = createValidProduct();
            
            Order order = Order.builder()
                    .discountAmount(validDiscountAmount)
                    .shippingFee(validShippingFee)
                    .user(user)
                    .build();
            
            // 상태를 CONFIRMED로 변경
            order.confirm();

            int initialSize = order.getOrderItems().size();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    order.addOrderItem(product, 1)
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("PENDING 상태의 주문만 주문 상품을 추가할 수 있습니다"));
            
            // OrderItem이 추가되지 않았는지 확인
            assertEquals(initialSize, order.getOrderItems().size(), "예외 발생 시 OrderItem이 추가되지 않아야 함");
        }

        @DisplayName("성공 케이스 : 여러 OrderItem을 순차적으로 추가해도 계산이 올바르게 됨")
        @Test
        void addOrderItem_multipleItems_Success() {
            // arrange
            User user = createValidUser();
            Product product = createValidProduct();
            
            Order order = Order.builder()
                    .discountAmount(validDiscountAmount)
                    .shippingFee(validShippingFee)
                    .user(user)
                    .build();
            
            // 첫 번째 OrderItem 추가
            order.addOrderItem(product, 2);

            BigDecimal initialTotalPrice = order.getTotalPrice();
            BigDecimal item1TotalAmount = product.getPrice().multiply(BigDecimal.valueOf(1));
            BigDecimal item2TotalAmount = product.getPrice().multiply(BigDecimal.valueOf(2));
            BigDecimal expectedTotalPrice = initialTotalPrice
                    .add(item1TotalAmount)
                    .add(item2TotalAmount);
            BigDecimal expectedFinalAmount = expectedTotalPrice
                    .subtract(validDiscountAmount)
                    .add(validShippingFee);

            // act
            order.addOrderItem(product, 1);
            order.addOrderItem(product, 2);

            // assert
            assertAll(
                    () -> assertEquals(3, order.getOrderItems().size(), "2개의 OrderItem이 추가되어 총 3개가 되어야 함"),
                    () -> assertEquals(expectedTotalPrice, order.getTotalPrice(), "totalPrice는 모든 OrderItem의 totalAmount 합계여야 함"),
                    () -> assertEquals(expectedFinalAmount, order.getFinalAmount(), "finalAmount는 totalPrice - discountAmount + shippingFee로 계산되어야 함")
            );
        }
    }
}
