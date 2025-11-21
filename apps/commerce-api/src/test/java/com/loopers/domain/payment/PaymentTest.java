package com.loopers.domain.payment;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.order.Order;
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

@DisplayName("Payment 테스트")
public class PaymentTest {

    // 고정 fixture
    private final PaymentMethod validMethod = PaymentMethod.POINT;
    private final PaymentStatus validPaymentStatus = PaymentStatus.PENDING;

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
        User user = createValidUser();
        Product product = createValidProduct();

        Order order = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .user(user)
                .build();
        
        // OrderItem 추가
        order.addOrderItem(product, 2);

        return order;
    }

    @DisplayName("Payment 엔티티 생성")
    @Nested
    class CreatePaymentTest {

        @DisplayName("성공 케이스 : 필드가 모두 형식에 맞으면 Payment 객체 생성 성공")
        @Test
        void createPayment_withValidFields_Success() {
            // arrange
            Order order = createValidOrder();
            BigDecimal expectedAmount = order.getFinalAmount(); // 생성 시 order.getFinalAmount()로 자동 설정됨

            // act
            Payment payment = Payment.builder()
                    .method(validMethod)
                    .paymentStatus(validPaymentStatus)
                    .order(order)
                    .build();

            // assert
            assertNotNull(payment);
            assertAll(
                    () -> assertEquals(validMethod, payment.getMethod()),
                    () -> assertEquals(expectedAmount, payment.getAmount(), "amount는 order.getFinalAmount()로 자동 설정되어야 함"),
                    () -> assertEquals(validPaymentStatus, payment.getPaymentStatus()),
                    () -> assertEquals(order, payment.getOrder())
            );
        }

        @DisplayName("실패 케이스 : method가 null이면 예외 발생")
        @Test
        void createPayment_withNullMethod_ThrowsException() {
            // arrange
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Payment.builder()
                            .method(null)
                            .paymentStatus(validPaymentStatus)
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("method가 비어있을 수 없습니다"));
        }


        @DisplayName("실패 케이스 : paymentStatus가 null이면 예외 발생")
        @Test
        void createPayment_withNullPaymentStatus_ThrowsException() {
            // arrange
            Order order = createValidOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Payment.builder()
                            .method(validMethod)
                            .paymentStatus(null)
                            .order(order)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("paymentStatus가 비어있을 수 없습니다"));
        }

        @DisplayName("실패 케이스 : order가 null이면 예외 발생")
        @Test
        void createPayment_withNullOrder_ThrowsException() {
            // arrange
            // order가 null이면 amount도 null이 되고, guard()에서 amount null 체크가 order null 체크보다 먼저 발생
            // 따라서 order가 null인 경우 amount null 예외가 먼저 발생할 수 있음
            
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Payment.builder()
                            .method(validMethod)
                            .paymentStatus(validPaymentStatus)
                            .order(null)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            // guard() 순서에 따라 amount null 예외 또는 order null 예외가 발생할 수 있음
            assertTrue(exception.getCustomMessage().contains("amount가 비어있을 수 없습니다") || 
                      exception.getCustomMessage().contains("order가 비어있을 수 없습니다"));
        }
    }
}

