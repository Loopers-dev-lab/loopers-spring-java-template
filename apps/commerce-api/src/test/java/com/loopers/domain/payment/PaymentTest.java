package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    @DisplayName("결제를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("유효한 정보로 생성하면, PENDING 상태의 결제가 생성된다.")
        @Test
        void createsPayment_whenValidInformationIsProvided() {
            // arrange
            String userId = "user123";
            Long orderId = 1L;
            CardType cardType = CardType.SAMSUNG;
            String cardNo = "1234-5678-9012-3456";
            BigDecimal amount = BigDecimal.valueOf(10000);

            // act
            Payment payment = Payment.create(userId, orderId, cardType, cardNo, amount);

            // assert
            assertAll(
                    () -> assertThat(payment.getUserId()).isEqualTo(userId),
                    () -> assertThat(payment.getOrderId()).isEqualTo(orderId),
                    () -> assertThat(payment.getCardType()).isEqualTo(cardType),
                    () -> assertThat(payment.getMaskedCardNo()).isEqualTo("1234-****-****-3456"),
                    () -> assertThat(payment.getAmount()).isEqualTo(amount),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING),
                    () -> assertThat(payment.getPgTransactionKey()).isNull(),
                    () -> assertThat(payment.isPending()).isTrue()
            );
        }

        @DisplayName("사용자 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenUserIdIsNull() {
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                Payment.create(null, 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000));
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("주문 ID가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenOrderIdIsZeroOrNegative() {
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                Payment.create("user123", 0L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000));
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("카드 타입이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenCardTypeIsNull() {
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                Payment.create("user123", 1L, null, "1234-5678-9012-3456", BigDecimal.valueOf(10000));
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("카드 번호 형식이 잘못되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenCardNoFormatIsInvalid() {
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                Payment.create("user123", 1L, CardType.SAMSUNG, "1234567890123456", BigDecimal.valueOf(10000));
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("결제 금액이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenAmountIsZeroOrNegative() {
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                Payment.create("user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.ZERO);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("PG 트랜잭션 키를 할당할 때,")
    @Nested
    class AssignPgTransactionKey {

        @DisplayName("유효한 트랜잭션 키를 할당하면, 정상적으로 저장된다.")
        @Test
        void assignsTransactionKey_whenValidKeyIsProvided() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            String transactionKey = "20250101:TR:123456";

            // act
            payment.assignPgTransactionKey(transactionKey);

            // assert
            assertThat(payment.getPgTransactionKey()).isEqualTo(transactionKey);
        }

        @DisplayName("이미 트랜잭션 키가 설정되어 있으면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflictException_whenTransactionKeyAlreadyAssigned() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.assignPgTransactionKey("20250101:TR:123456");

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                payment.assignPgTransactionKey("20250101:TR:999999");
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("트랜잭션 키가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenTransactionKeyIsNull() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                payment.assignPgTransactionKey(null);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("결제를 성공 처리할 때,")
    @Nested
    class Success {

        @DisplayName("PENDING 상태에서 성공 처리하면, SUCCESS 상태로 변경된다.")
        @Test
        void changesStatusToSuccess_whenPending() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );

            // act
            payment.success();

            // assert
            assertAll(
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS),
                    () -> assertThat(payment.isSuccess()).isTrue(),
                    () -> assertThat(payment.getFailureReason()).isNull()
            );
        }

        @DisplayName("이미 SUCCESS 상태이면, 멱등하게 처리된다.")
        @Test
        void doesNothing_whenAlreadySuccess() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.success();

            // act
            payment.success(); // 멱등성 테스트

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @DisplayName("FAILED 상태에서 성공 처리하면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflictException_whenAlreadyFailed() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.fail("카드 한도 초과");

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                payment.success();
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("결제를 실패 처리할 때,")
    @Nested
    class Fail {

        @DisplayName("PENDING 상태에서 실패 처리하면, FAILED 상태로 변경된다.")
        @Test
        void changesStatusToFailed_whenPending() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            String reason = "카드 한도 초과";

            // act
            payment.fail(reason);

            // assert
            assertAll(
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                    () -> assertThat(payment.isFailed()).isTrue(),
                    () -> assertThat(payment.getFailureReason()).isEqualTo(reason)
            );
        }

        @DisplayName("이미 FAILED 상태이면, 멱등하게 처리된다.")
        @Test
        void doesNothing_whenAlreadyFailed() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.fail("카드 한도 초과");

            // act
            payment.fail("잘못된 카드 번호"); // 멱등성 테스트

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @DisplayName("SUCCESS 상태에서 실패 처리하면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsConflictException_whenAlreadySuccess() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );
            payment.success();

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                payment.fail("카드 한도 초과");
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("결제 소유자를 확인할 때,")
    @Nested
    class IsOwnedBy {

        @DisplayName("동일한 사용자 ID이면, true를 반환한다.")
        @Test
        void returnsTrue_whenUserIdMatches() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );

            // act & assert
            assertThat(payment.isOwnedBy("user123")).isTrue();
        }

        @DisplayName("다른 사용자 ID이면, false를 반환한다.")
        @Test
        void returnsFalse_whenUserIdDoesNotMatch() {
            // arrange
            Payment payment = Payment.create(
                    "user123", 1L, CardType.SAMSUNG, "1234-5678-9012-3456", BigDecimal.valueOf(10000)
            );

            // act & assert
            assertThat(payment.isOwnedBy("user999")).isFalse();
        }
    }
}
