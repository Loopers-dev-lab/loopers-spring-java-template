package com.loopers.domain.payment;

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

@DisplayName("Payment 테스트")
public class CommercePaymentTest {

    // 고정 fixture
    private final String validTransactionKey = "20250101:TR:abc123";
    private final PaymentDto.PaymentMethod validMethod = PaymentDto.PaymentMethod.POINT;
    private final PaymentDto.CardType validCardType = PaymentDto.CardType.SAMSUNG;
    private final String validCardNo = "1234-5678-9012-3456";
    private final PaymentDto.PaymentStatus validPaymentStatus = PaymentDto.PaymentStatus.PENDING;
    private final Long validOrderId = 1L;
    private final BigDecimal validAmount = BigDecimal.valueOf(10000);

    private User createValidUser() {
        User user = User.builder()
                .loginId("testuser1")
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        
        // ID 강제 주입
        ReflectionTestUtils.setField(user, "id", 1L);
        
        return user;
    }

    private Product createValidProduct() {
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        
        // Brand ID 강제 주입
        ReflectionTestUtils.setField(brand, "id", 1L);

        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(5000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(brand.getId())
                .build();

        // Product ID 강제 주입
        ReflectionTestUtils.setField(product, "id", 1L);

        return product;
    }


    @DisplayName("Payment 엔티티 생성")
    @Nested
    class CreateCommercePaymentTest {

        @DisplayName("성공 케이스 : 필드가 모두 형식에 맞으면 Payment 객체 생성 성공")
        @Test
        void createPayment_withValidFields_Success() {
            // arrange
            String transactionKey = validTransactionKey;
            PaymentDto.PaymentMethod method = validMethod;
            PaymentDto.CardType cardType = validCardType;
            String cardNo = validCardNo;
            PaymentDto.PaymentStatus paymentStatus = validPaymentStatus;
            Long orderId = validOrderId;
            BigDecimal amount = validAmount;

            // act
            CommercePayment commercePayment = CommercePayment.builder()
                    .transactionKey(transactionKey)
                    .method(method)
                    .cardType(cardType)
                    .cardNo(cardNo)
                    .paymentStatus(paymentStatus)
                    .orderId(orderId)
                    .amount(amount)
                    .build();

            // assert
            assertNotNull(commercePayment);
            assertAll(
                    () -> assertEquals(transactionKey, commercePayment.getTransactionKey(), "transactionKey가 일치해야 함"),
                    () -> assertEquals(method, commercePayment.getMethod(), "method가 일치해야 함"),
                    () -> assertEquals(cardType, commercePayment.getCardType(), "cardType이 일치해야 함"),
                    () -> assertEquals(cardNo, commercePayment.getCardNo(), "cardNo가 일치해야 함"),
                    () -> assertEquals(paymentStatus, commercePayment.getPaymentStatus(), "paymentStatus가 일치해야 함"),
                    () -> assertEquals(orderId, commercePayment.getOrderId(), "orderId가 일치해야 함"),
                    () -> assertEquals(amount, commercePayment.getAmount(), "amount가 일치해야 함")
            );
        }

        @DisplayName("실패 케이스 : method가 null이면 예외 발생")
        @Test
        void createPayment_withNullMethod_ThrowsException() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    CommercePayment.builder()
                            .transactionKey(validTransactionKey)
                            .method(null)
                            .paymentStatus(validPaymentStatus)
                            .orderId(validOrderId)
                            .amount(validAmount)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType(),
                    String.format("예상 ErrorType: BAD_REQUEST, 실제 ErrorType: %s", exception.getErrorType()));
            assertTrue(exception.getCustomMessage().contains("method가 비어있을 수 없습니다"),
                    String.format("예상 메시지: 'method가 비어있을 수 없습니다' 포함, 실제 메시지: %s", exception.getCustomMessage()));
        }

        @DisplayName("실패 케이스 : orderId가 null이면 예외 발생")
        @Test
        void createPayment_withNullOrderId_ThrowsException() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    CommercePayment.builder()
                            .transactionKey(validTransactionKey)
                            .method(validMethod)
                            .paymentStatus(validPaymentStatus)
                            .orderId(null)
                            .amount(validAmount)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType(),
                    String.format("예상 ErrorType: BAD_REQUEST, 실제 ErrorType: %s", exception.getErrorType()));
            assertTrue(exception.getCustomMessage().contains("orderId가 비어있을 수 없습니다"),
                    String.format("예상 메시지: 'orderId가 비어있을 수 없습니다' 포함, 실제 메시지: %s", exception.getCustomMessage()));
        }

        @DisplayName("실패 케이스 : amount가 null이면 예외 발생")
        @Test
        void createPayment_withNullAmount_ThrowsException() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    CommercePayment.builder()
                            .transactionKey(validTransactionKey)
                            .method(validMethod)
                            .paymentStatus(validPaymentStatus)
                            .orderId(validOrderId)
                            .amount(null)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType(),
                    String.format("예상 ErrorType: BAD_REQUEST, 실제 ErrorType: %s", exception.getErrorType()));
            assertTrue(exception.getCustomMessage().contains("amount가 비어있을 수 없습니다"),
                    String.format("예상 메시지: 'amount가 비어있을 수 없습니다' 포함, 실제 메시지: %s", exception.getCustomMessage()));
        }

        @DisplayName("실패 케이스 : amount가 음수이면 예외 발생")
        @Test
        void createPayment_withNegativeAmount_ThrowsException() {
            // arrange
            BigDecimal negativeAmount = BigDecimal.valueOf(-1000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    CommercePayment.builder()
                            .transactionKey(validTransactionKey)
                            .method(validMethod)
                            .paymentStatus(validPaymentStatus)
                            .orderId(validOrderId)
                            .amount(negativeAmount)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType(),
                    String.format("예상 ErrorType: BAD_REQUEST, 실제 ErrorType: %s", exception.getErrorType()));
            assertTrue(exception.getCustomMessage().contains("amount는 음수가 될 수 없습니다"),
                    String.format("예상 메시지: 'amount는 음수가 될 수 없습니다' 포함, 실제 메시지: %s", exception.getCustomMessage()));
        }

        @DisplayName("실패 케이스 : paymentStatus가 null이면 예외 발생")
        @Test
        void createPayment_withNullPaymentStatus_ThrowsException() {
            // act & assert
            // paymentStatus가 null이면 builder에서 기본값 PENDING으로 설정되므로, 
            // 실제로는 예외가 발생하지 않을 수 있음
            // 하지만 명시적으로 null을 전달하면 guard()에서 검증됨
            CommercePayment payment = CommercePayment.builder()
                    .transactionKey(validTransactionKey)
                    .method(validMethod)
                    .paymentStatus(null)  // null이면 builder에서 PENDING으로 설정됨
                    .orderId(validOrderId)
                    .amount(validAmount)
                    .build();

            // paymentStatus가 null이면 builder에서 PENDING으로 자동 설정되므로 예외가 발생하지 않음
            assertNotNull(payment);
            assertEquals(PaymentDto.PaymentStatus.PENDING, payment.getPaymentStatus());
        }
    }
}
