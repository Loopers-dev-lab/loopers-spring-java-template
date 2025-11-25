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
import org.springframework.test.util.ReflectionTestUtils;

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

        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(5000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(brand.getId())
                .build();
        
        // ID 강제 주입 (ReflectionTestUtils 등 사용)
        ReflectionTestUtils.setField(product, "id", 1L);

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
                .userId(user.getId())
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
            BigDecimal expectedProductPrice = product.getPrice(); // 생성 시 productPrice로 설정됨
            BigDecimal expectedTotalAmount = expectedProductPrice.multiply(BigDecimal.valueOf(validQuantity)); // productPrice * quantity로 자동 계산됨

            // act
            OrderItem orderItem = OrderItem.builder()
                    .quantity(validQuantity)
                    .productId(product.getId())
                    .productName(product.getName())
                    .productPrice(product.getPrice())
                    .order(order)
                    .build();

            // assert
            assertNotNull(orderItem);
            assertAll(
                    () -> assertEquals(validQuantity, orderItem.getQuantity()),
                    () -> assertEquals(expectedTotalAmount, orderItem.getTotalAmount(), "totalAmount는 productPrice * quantity로 자동 계산되어야 함"),
                    () -> assertEquals(product.getId(), orderItem.getProductId()),
                    () -> assertEquals(product.getName(), orderItem.getProductName()),
                    () -> assertEquals(product.getPrice(), orderItem.getProductPrice()),
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
                            .productId(product.getId())
                            .productName(product.getName())
                            .productPrice(product.getPrice())
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertEquals("OrderItem : quantity가 비어있을 수 없습니다.", exception.getCustomMessage(),
                    String.format("예상 메시지: 'OrderItem : quantity가 비어있을 수 없습니다.', 실제 메시지: %s", exception.getCustomMessage()));
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
                            .productId(product.getId())
                            .productName(product.getName())
                            .productPrice(product.getPrice())
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertEquals("OrderItem : quantity는 0보다 커야 합니다.", exception.getCustomMessage(),
                    String.format("예상 메시지: 'OrderItem : quantity는 0보다 커야 합니다.', 실제 메시지: %s", exception.getCustomMessage()));
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
                            .productId(product.getId())
                            .productName(product.getName())
                            .productPrice(product.getPrice())
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertEquals("OrderItem : quantity는 0보다 커야 합니다.", exception.getCustomMessage(),
                    String.format("예상 메시지: 'OrderItem : quantity는 0보다 커야 합니다.', 실제 메시지: %s", exception.getCustomMessage()));
        }

        // productPrice와 totalAmount는 자동으로 계산되므로 직접 설정할 수 없음
        // 따라서 productPrice와 totalAmount에 대한 실패 케이스 테스트는 제거됨

        @DisplayName("실패 케이스 : productId가 null이면 예외 발생")
        @Test
        void createOrderItem_withNullProductId_ThrowsException() {
            // arrange
            Product product = createValidProduct();
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(validQuantity)
                            .productId(null)
                            .productName(product.getName())
                            .productPrice(product.getPrice())
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertEquals("OrderItem : productId가 비어있을 수 없습니다.", exception.getCustomMessage(),
                    String.format("예상 메시지: 'OrderItem : productId가 비어있을 수 없습니다.', 실제 메시지: %s", exception.getCustomMessage()));
        }

        @DisplayName("실패 케이스 : productName이 null이면 예외 발생")
        @Test
        void createOrderItem_withNullProductName_ThrowsException() {
            // arrange
            Product product = createValidProduct();
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(validQuantity)
                            .productId(product.getId())
                            .productName(null)
                            .productPrice(product.getPrice())
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertEquals("OrderItem : productName이 비어있을 수 없습니다.", exception.getCustomMessage(),
                    String.format("예상 메시지: 'OrderItem : productName이 비어있을 수 없습니다.', 실제 메시지: %s", exception.getCustomMessage()));
        }

        @DisplayName("실패 케이스 : productPrice가 null이면 예외 발생")
        @Test
        void createOrderItem_withNullProductPrice_ThrowsException() {
            // arrange
            Product product = createValidProduct();
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    OrderItem.builder()
                            .quantity(validQuantity)
                            .productId(product.getId())
                            .productName(product.getName())
                            .productPrice(null)
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertEquals("OrderItem : productPrice가 비어있을 수 없습니다.", exception.getCustomMessage(),
                    String.format("예상 메시지: 'OrderItem : productPrice가 비어있을 수 없습니다.', 실제 메시지: %s", exception.getCustomMessage()));
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
                            .productId(product.getId())
                            .productName(product.getName())
                            .productPrice(product.getPrice())
                            .order(null)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertEquals("OrderItem : order가 비어있을 수 없습니다.", exception.getCustomMessage(),
                    String.format("예상 메시지: 'OrderItem : order가 비어있을 수 없습니다.', 실제 메시지: %s", exception.getCustomMessage()));
        }
    }

}

