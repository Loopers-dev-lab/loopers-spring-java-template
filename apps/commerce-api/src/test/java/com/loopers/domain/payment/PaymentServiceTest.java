package com.loopers.domain.payment;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment Service 테스트")
@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CommercePaymentRepository commercePaymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long testUserId;
    private Long testOrderId;
    private String testTransactionKey;

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

        // 테스트용 Order 생성
        Order testOrder = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .userId(testUserId)
                .build();
        Order savedOrder = orderService.saveOrder(testOrder);
        testOrderId = savedOrder.getId();

        testTransactionKey = "TEST_TRANSACTION_" + System.currentTimeMillis();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("saveCommercePayment 테스트")
    @Nested
    class SaveCommercePaymentTest {

        @DisplayName("성공 케이스: CommercePayment 저장 성공")
        @Test
        void saveCommercePayment_withValidPayment_Success() {
            // arrange
            CommercePayment payment = CommercePayment.builder()
                    .transactionKey(testTransactionKey)
                    .method(PaymentDto.PaymentMethod.CARD)
                    .cardType(PaymentDto.CardType.SAMSUNG)
                    .cardNo("1111-2222-3333-4444")
                    .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                    .orderId(testOrderId)
                    .amount(BigDecimal.valueOf(10000))
                    .build();

            // act
            CommercePayment savedPayment = paymentService.saveCommercePayment(payment);

            // assert
            assertNotNull(savedPayment);
            assertNotNull(savedPayment.getId(), "결제 ID는 null이 아니어야 함");
            assertEquals(testTransactionKey, savedPayment.getTransactionKey(), "거래 키가 일치해야 함");
            assertEquals(PaymentDto.PaymentMethod.CARD, savedPayment.getMethod(), "결제 방법이 일치해야 함");
            assertEquals(PaymentDto.PaymentStatus.PENDING, savedPayment.getPaymentStatus(), "결제 상태가 일치해야 함");
            assertEquals(testOrderId, savedPayment.getOrderId(), "주문 ID가 일치해야 함");
        }
    }

    @DisplayName("saveSuccessPayment 테스트")
    @Nested
    class SaveSuccessPaymentTest {

        @DisplayName("성공 케이스: 결제 상태 SUCCESS로 변경 성공")
        @Test
        void saveSuccessPayment_withPendingPayment_Success() {
            // arrange
            CommercePayment payment = CommercePayment.builder()
                    .transactionKey(testTransactionKey)
                    .method(PaymentDto.PaymentMethod.CARD)
                    .cardType(PaymentDto.CardType.SAMSUNG)
                    .cardNo("1111-2222-3333-4444")
                    .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                    .orderId(testOrderId)
                    .amount(BigDecimal.valueOf(10000))
                    .build();
            CommercePayment savedPayment = paymentService.saveCommercePayment(payment);
            assertEquals(PaymentDto.PaymentStatus.PENDING, savedPayment.getPaymentStatus(), "초기 결제 상태는 PENDING이어야 함");

            // act
            paymentService.saveSuccessPayment(testTransactionKey);

            // assert
            CommercePayment foundPayment = paymentService.findByTransactionKey(testTransactionKey);
            assertEquals(PaymentDto.PaymentStatus.SUCCESS, foundPayment.getPaymentStatus(), "결제 상태는 SUCCESS여야 함");
        }

        @DisplayName("실패 케이스: 존재하지 않는 transactionKey에 대해 성공 처리 시도 시 NOT_FOUND 예외 발생")
        @Test
        void saveSuccessPayment_withNonExistentTransactionKey_NotFound() {
            // arrange
            String nonExistentTransactionKey = "NON_EXISTENT_KEY";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    paymentService.saveSuccessPayment(nonExistentTransactionKey)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType(),
                    String.format("예상 ErrorType: NOT_FOUND, 실제 ErrorType: %s", exception.getErrorType()));
            assertTrue(exception.getCustomMessage().contains("CommercePayment를 찾을 수 없습니다"),
                    String.format("예상 메시지: 'CommercePayment를 찾을 수 없습니다' 포함, 실제 메시지: %s", exception.getCustomMessage()));
        }
    }

    @DisplayName("saveFailedPayment 테스트")
    @Nested
    class SaveFailedPaymentTest {

        @DisplayName("성공 케이스: 결제 상태 FAILED로 변경 및 실패 사유 저장 성공")
        @Test
        void saveFailedPayment_withPendingPayment_Success() {
            // arrange
            CommercePayment payment = CommercePayment.builder()
                    .transactionKey(testTransactionKey)
                    .method(PaymentDto.PaymentMethod.CARD)
                    .cardType(PaymentDto.CardType.SAMSUNG)
                    .cardNo("1111-2222-3333-4444")
                    .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                    .orderId(testOrderId)
                    .amount(BigDecimal.valueOf(10000))
                    .build();
            CommercePayment savedPayment = paymentService.saveCommercePayment(payment);
            assertEquals(PaymentDto.PaymentStatus.PENDING, savedPayment.getPaymentStatus(), "초기 결제 상태는 PENDING이어야 함");
            String failureReason = "결제 요청에 실패했습니다";

            // act
            paymentService.saveFailedPayment(testTransactionKey, failureReason);

            // assert
            CommercePayment foundPayment = paymentService.findByTransactionKey(testTransactionKey);
            assertEquals(PaymentDto.PaymentStatus.FAILED, foundPayment.getPaymentStatus(), "결제 상태는 FAILED여야 함");
            assertEquals(failureReason, foundPayment.getMessage(), "실패 사유가 저장되어야 함");
        }

        @DisplayName("실패 케이스: 존재하지 않는 transactionKey에 대해 실패 처리 시도 시 NOT_FOUND 예외 발생")
        @Test
        void saveFailedPayment_withNonExistentTransactionKey_NotFound() {
            // arrange
            String nonExistentTransactionKey = "NON_EXISTENT_KEY";
            String failureReason = "결제 요청에 실패했습니다";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    paymentService.saveFailedPayment(nonExistentTransactionKey, failureReason)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType(),
                    String.format("예상 ErrorType: NOT_FOUND, 실제 ErrorType: %s", exception.getErrorType()));
            assertTrue(exception.getCustomMessage().contains("CommercePayment를 찾을 수 없습니다"),
                    String.format("예상 메시지: 'CommercePayment를 찾을 수 없습니다' 포함, 실제 메시지: %s", exception.getCustomMessage()));
        }
    }
}
