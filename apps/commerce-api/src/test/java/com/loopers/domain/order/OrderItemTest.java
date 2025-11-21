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

@DisplayName("OrderItem 테스트")
public class OrderItemTest {

    // 고정 fixture
    private final Integer validQuantity = 2;

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

    private Order createValidOrder() {
        User user = User.builder()
                .loginId("testuser1")
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();

        Order order = Order.builder()
                .discountAmount(BigDecimal.valueOf(1000))
                .shippingFee(BigDecimal.valueOf(2000))
                .user(user)
                .build();

        return order;
    }

    @DisplayName("OrderItem 엔티티 생성")
    @Nested
    class CreateOrderItemTest {

        @DisplayName("성공 케이스 : 필드가 모두 형식에 맞으면 OrderItem 객체 생성 성공")
        @Test
        void createOrderItem_withValidFields_Success() {
            // arrange
            Product product = createValidProduct();
            Order order = createValidOrder();
            BigDecimal expectedUnitPrice = product.getPrice(); // 생성 시 product.getPrice()로 자동 설정됨
            BigDecimal expectedTotalAmount = expectedUnitPrice.multiply(BigDecimal.valueOf(validQuantity)); // unitPrice * quantity로 자동 계산됨

            // act
            OrderItem orderItem = OrderItem.builder()
                    .quantity(validQuantity)
                    .product(product)
                    .order(order)
                    .build();

            // assert
            assertNotNull(orderItem);
            assertAll(
                    () -> assertEquals(validQuantity, orderItem.getQuantity()),
                    () -> assertEquals(expectedUnitPrice, orderItem.getUnitPrice(), "unitPrice는 product.getPrice()로 자동 설정되어야 함"),
                    () -> assertEquals(expectedTotalAmount, orderItem.getTotalAmount(), "totalAmount는 unitPrice * quantity로 자동 계산되어야 함"),
                    () -> assertEquals(product, orderItem.getProduct()),
                    () -> assertEquals(order, orderItem.getOrder())
            );
        }

        @DisplayName("실패 케이스 : quantity가 null이면 예외 발생")
        @Test
        void createOrderItem_withNullQuantity_ThrowsException() {
            // arrange
            Product product = createValidProduct();
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(null)
                            .product(product)
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("quantity가 비어있을 수 없습니다"));
        }

        @DisplayName("실패 케이스 : quantity가 0이면 예외 발생")
        @Test
        void createOrderItem_withZeroQuantity_ThrowsException() {
            // arrange
            Product product = createValidProduct();
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(0)
                            .product(product)
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("quantity는 0보다 커야 합니다"));
        }

        @DisplayName("실패 케이스 : quantity가 음수이면 예외 발생")
        @Test
        void createOrderItem_withNegativeQuantity_ThrowsException() {
            // arrange
            Product product = createValidProduct();
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(-1)
                            .product(product)
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("quantity는 0보다 커야 합니다"));
        }

        // unitPrice와 totalAmount는 자동으로 계산되므로 직접 설정할 수 없음
        // 따라서 unitPrice와 totalAmount에 대한 실패 케이스 테스트는 제거됨

        @DisplayName("실패 케이스 : product가 null이면 예외 발생")
        @Test
        void createOrderItem_withNullProduct_ThrowsException() {
            // arrange
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(validQuantity)
                            .product(null)
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("product가 비어있을 수 없습니다"));
        }

        @DisplayName("실패 케이스 : order가 null이면 예외 발생")
        @Test
        void createOrderItem_withNullOrder_ThrowsException() {
            // arrange
            Product product = createValidProduct();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(validQuantity)
                            .product(product)
                            .order(null)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("order가 비어있을 수 없습니다"));
        }
    }

}

