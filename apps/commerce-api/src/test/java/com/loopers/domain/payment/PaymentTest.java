package com.loopers.domain.payment;

import com.loopers.domain.payment.fixture.PaymentFixture;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    @Nested
    @DisplayName("결제 생성 관련 테스트")
    class CreateTest {

        @DisplayName("정상적인 값으로 결제를 생성할 수 있다")
        @Test
        void create_withValidValues() {
            // arrange

            // act
            PaymentModel payment = PaymentFixture.createPayment();

            // assert
            assertAll(
                    () -> assertThat(payment).isNotNull(),
                    () -> assertThat(payment.getOrderIdValue()).isEqualTo(PaymentFixture.DEFAULT_ORDER_ID),
                    () -> assertThat(payment.getAmountValue()).isEqualTo(PaymentFixture.DEFAULT_AMOUNT),
                    () -> assertThat(payment.getMethodValue()).isEqualTo(PaymentFixture.DEFAULT_METHOD),
                    () -> assertThat(payment.getPointsUsedValue()).isEqualTo(PaymentFixture.DEFAULT_POINTS_USED),
                    () -> assertThat(payment.getStatusValue()).isEqualTo("INITIATED"),
                    () -> assertThat(payment.isPending()).isTrue()
            );
        }

        @DisplayName("주문 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenOrderIdNull() {
            // arrange
            Long orderId = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                PaymentFixture.createPaymentWithOrderId(orderId);
            });

            assertThat(exception.getMessage()).contains("주문 ID는 필수입니다");
        }

        @DisplayName("주문 ID가 0 이하이면 생성에 실패한다")
        @Test
        void create_whenOrderIdZeroOrNegative() {
            // arrange
            Long orderId = 0L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                PaymentFixture.createPaymentWithOrderId(orderId);
            });

            assertThat(exception.getMessage()).contains("주문 ID는 양수여야 합니다");
        }

        @DisplayName("결제 금액이 null이면 생성에 실패한다")
        @Test
        void create_whenAmountNull() {
            // arrange
            BigDecimal amount = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                PaymentFixture.createPaymentWithAmount(amount);
            });

            assertThat(exception.getMessage()).contains("결제 금액은 필수입니다");
        }

        @DisplayName("결제 금액이 음수이면 생성에 실패한다")
        @Test
        void create_whenAmountNegative() {
            // arrange
            BigDecimal amount = new BigDecimal("-1000");

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                PaymentFixture.createPaymentWithAmount(amount);
            });

            assertThat(exception.getMessage()).contains("결제 금액은 0 이상이어야 합니다");
        }

        @DisplayName("잘못된 결제 수단으로 생성에 실패한다")
        @Test
        void create_whenInvalidMethod() {
            // arrange
            String method = "INVALID_METHOD";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                PaymentFixture.createPaymentWithMethod(method);
            });

            assertThat(exception.getMessage()).contains("결제 수단을 확인해 주세요");
        }

        @DisplayName("포인트 사용량이 null이면 0으로 처리된다")
        @Test
        void create_whenPointsUsedNull() {
            // arrange
            BigDecimal pointsUsed = null;

            // act
            PaymentModel payment = PaymentFixture.createPaymentWithPointsUsed(pointsUsed);

            // assert
            assertThat(payment.getPointsUsedValue()).isEqualTo(BigDecimal.ZERO);
        }

        @DisplayName("포인트 사용량이 음수이면 생성에 실패한다")
        @Test
        void create_whenPointsUsedNegative() {
            // arrange
            BigDecimal pointsUsed = new BigDecimal("-500");

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                PaymentFixture.createPaymentWithPointsUsed(pointsUsed);
            });

            assertThat(exception.getMessage()).contains("사용된 포인트는 0 이상이어야 합니다");
        }
    }

    @Nested
    @DisplayName("결제 상태 변경 관련 테스트")
    class StatusChangeTest {

        @DisplayName("결제를 완료 처리할 수 있다")
        @Test
        void complete_success() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();
            String transactionId = PaymentFixture.DEFAULT_TRANSACTION_ID;

            // act
            payment.complete(transactionId);

            // assert
            assertAll(
                    () -> assertThat(payment.isCompleted()).isTrue(),
                    () -> assertThat(payment.getStatusValue()).isEqualTo("COMPLETED"),
                    () -> assertThat(payment.getTransactionIdValue()).isEqualTo(transactionId),
                    () -> assertThat(payment.getCompletedAt()).isNotNull()
            );
        }

        @DisplayName("이미 완료된 결제는 다시 완료 처리할 수 없다")
        @Test
        void complete_whenAlreadyCompleted() {
            // arrange
            PaymentModel payment = PaymentFixture.createCompletedPayment();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                payment.complete("NEW_TXN_ID");
            });

            assertThat(exception.getMessage()).contains("이미 완료된 결제입니다");
        }

        @DisplayName("결제를 실패 처리할 수 있다")
        @Test
        void fail_success() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();
            String reason = "카드 승인 거절";

            // act
            payment.fail(reason);

            // assert
            assertAll(
                    () -> assertThat(payment.isFailed()).isTrue(),
                    () -> assertThat(payment.getStatusValue()).isEqualTo("FAILED")
            );
        }

        @DisplayName("완료된 결제는 실패 처리할 수 없다")
        @Test
        void fail_whenAlreadyCompleted() {
            // arrange
            PaymentModel payment = PaymentFixture.createCompletedPayment();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                payment.fail("실패 처리 시도");
            });

            assertThat(exception.getMessage()).contains("완료된 결제는 실패 처리할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("결제 상태 확인 관련 테스트")
    class StatusCheckTest {

        @DisplayName("생성 직후 결제는 대기 상태이다")
        @Test
        void isPending_afterCreation() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();

            // act & assert
            assertAll(
                    () -> assertThat(payment.isPending()).isTrue(),
                    () -> assertThat(payment.isCompleted()).isFalse(),
                    () -> assertThat(payment.isFailed()).isFalse()
            );
        }

        @DisplayName("완료된 결제 상태를 올바르게 확인한다")
        @Test
        void isCompleted_check() {
            // arrange
            PaymentModel payment = PaymentFixture.createCompletedPayment();

            // act & assert
            assertAll(
                    () -> assertThat(payment.isCompleted()).isTrue(),
                    () -> assertThat(payment.isPending()).isFalse(),
                    () -> assertThat(payment.isFailed()).isFalse()
            );
        }

        @DisplayName("실패한 결제 상태를 올바르게 확인한다")
        @Test
        void isFailed_check() {
            // arrange
            PaymentModel payment = PaymentFixture.createFailedPayment();

            // act & assert
            assertAll(
                    () -> assertThat(payment.isFailed()).isTrue(),
                    () -> assertThat(payment.isPending()).isFalse(),
                    () -> assertThat(payment.isCompleted()).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("PG사 콜백 검증 관련 테스트")
    class CallbackValidationTest {

        @DisplayName("올바른 콜백 정보로 검증에 성공한다")
        @Test
        void validateCallback_withValidData() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();
            BigDecimal callbackAmount = PaymentFixture.DEFAULT_AMOUNT;
            String orderNumber = "ORDER20241201001";

            // act
            boolean result = payment.validateCallback(callbackAmount, orderNumber);

            // assert
            assertThat(result).isTrue();
        }

        @DisplayName("금액이 일치하지 않으면 검증에 실패한다")
        @Test
        void validateCallback_withWrongAmount() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();
            BigDecimal wrongAmount = new BigDecimal("30000");
            String orderNumber = "ORDER20241201001";

            // act
            boolean result = payment.validateCallback(wrongAmount, orderNumber);

            // assert
            assertThat(result).isFalse();
        }

        @DisplayName("콜백 금액이 null이면 검증에 실패한다")
        @Test
        void validateCallback_withNullAmount() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();
            BigDecimal callbackAmount = null;
            String orderNumber = "ORDER20241201001";

            // act
            boolean result = payment.validateCallback(callbackAmount, orderNumber);

            // assert
            assertThat(result).isFalse();
        }

        @DisplayName("주문 번호가 null이면 검증에 실패한다")
        @Test
        void validateCallback_withNullOrderNumber() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();
            BigDecimal callbackAmount = PaymentFixture.DEFAULT_AMOUNT;
            String orderNumber = null;

            // act
            boolean result = payment.validateCallback(callbackAmount, orderNumber);

            // assert
            assertThat(result).isFalse();
        }

        @DisplayName("주문 번호가 빈 문자열이면 검증에 실패한다")
        @Test
        void validateCallback_withEmptyOrderNumber() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();
            BigDecimal callbackAmount = PaymentFixture.DEFAULT_AMOUNT;
            String orderNumber = "   ";

            // act
            boolean result = payment.validateCallback(callbackAmount, orderNumber);

            // assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("결제 정보 조회 관련 테스트")
    class InfoRetrievalTest {

        @DisplayName("결제 정보를 올바르게 조회할 수 있다")
        @Test
        void getPaymentInfo() {
            // arrange
            PaymentModel payment = PaymentFixture.createMixedPayment();

            // act & assert
            assertAll(
                    () -> assertThat(payment.getOrderIdValue()).isEqualTo(PaymentFixture.DEFAULT_ORDER_ID),
                    () -> assertThat(payment.getAmountValue()).isEqualTo(PaymentFixture.DEFAULT_AMOUNT),
                    () -> assertThat(payment.getAmountIntValue()).isEqualTo(PaymentFixture.DEFAULT_AMOUNT.intValue()),
                    () -> assertThat(payment.getPointsUsedValue()).isEqualTo(new BigDecimal("10000")),
                    () -> assertThat(payment.getPointsUsedIntValue()).isEqualTo(10000),
                    () -> assertThat(payment.getMethodValue()).isEqualTo("MIXED")
            );
        }

        @DisplayName("트랜잭션 ID는 완료 전까지 비어있다")
        @Test
        void getTransactionId_beforeCompletion() {
            // arrange
            PaymentModel payment = PaymentFixture.createPayment();

            // act & assert
            assertThat(payment.getTransactionIdValue()).isNull();
        }

        @DisplayName("완료 후에는 트랜잭션 ID를 조회할 수 있다")
        @Test
        void getTransactionId_afterCompletion() {
            // arrange
            PaymentModel payment = PaymentFixture.createCompletedPayment();

            // act & assert
            assertThat(payment.getTransactionIdValue()).isEqualTo(PaymentFixture.DEFAULT_TRANSACTION_ID);
        }
    }
}
