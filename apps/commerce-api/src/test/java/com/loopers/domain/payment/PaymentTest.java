package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    @Nested
    @DisplayName("포인트 결제 생성 (Payment.createForPoint)")
    class CreatePointPayment {

        @DisplayName("포인트 결제를 생성할 수 있다.")
        @Test
        void createPointPayment() {
            // given
            Long orderId = 1L;
            Long userId = 100L;
            Long amount = 50000L;
            String idempotencyKey = "order-1-user-100-point-20250105";

            // when
            Payment payment = Payment.createForPoint(orderId, userId, amount, idempotencyKey);

            // then
            assertThat(payment.getOrderId()).isEqualTo(orderId);
            assertThat(payment.getUserId()).isEqualTo(userId);
            assertThat(payment.getAmountValue()).isEqualTo(amount);
            assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.POINT);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
            assertThat(payment.isSuccess()).isTrue();
        }

        @DisplayName("주문 ID가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenOrderIdIsNull() {
            assertThatThrownBy(() -> Payment.createForPoint(null, 100L, 1000L, "key-123"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("주문 ID는 필수")
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("사용자 ID가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenUserIdIsNull() {
            assertThatThrownBy(() -> Payment.createForPoint(1L, null, 1000L, "key-123"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용자 ID는 필수")
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("금액이 0 이하이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(longs = {0, -1, -1000})
        void throwsException_whenAmountIsZeroOrNegative(Long amount) {
            assertThatThrownBy(() -> Payment.createForPoint(1L, 100L, amount, "key-123"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("결제 금액은 0보다 커야")
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("멱등성 키가 null이거나 빈 값이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        void throwsException_whenIdempotencyKeyIsBlank(String idempotencyKey) {
            assertThatThrownBy(() -> Payment.createForPoint(1L, 100L, 1000L, idempotencyKey))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("멱등성 키")
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("멱등성 키가 100자를 초과하면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenIdempotencyKeyIsTooLong() {
            String longKey = "a".repeat(101);

            assertThatThrownBy(() -> Payment.createForPoint(1L, 100L, 1000L, longKey))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("100자를 초과")
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("멱등성 키가 허용되지 않는 문자를 포함하면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"key@123", "key#123", "key!123", "키-123", "key 123"})
        void throwsException_whenIdempotencyKeyContainsInvalidChars(String idempotencyKey) {
            assertThatThrownBy(() -> Payment.createForPoint(1L, 100L, 1000L, idempotencyKey))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("영문, 숫자, -, _, : 만")
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("PG 카드 결제 생성 (Payment.createForPgCard)")
    class CreatePgCardPayment {

        @DisplayName("PG 카드 결제를 생성할 수 있다.")
        @Test
        void createPgCardPayment() {
            // given
            Long orderId = 1L;
            Long userId = 100L;
            String cardType = "SAMSUNG";
            String cardNo = "1234-5678-9012-3456";
            Long amount = 50000L;
            String callbackUrl = "http://localhost:8080/api/v1/payments/callback";
            String idempotencyKey = "order-1-user-100-card-20250105";

            // when
            Payment payment = Payment.createForPgCard(
                    orderId, userId, cardType, cardNo, amount, callbackUrl, idempotencyKey
            );

            // then
            assertThat(payment.getOrderId()).isEqualTo(orderId);
            assertThat(payment.getUserId()).isEqualTo(userId);
            assertThat(payment.getAmountValue()).isEqualTo(amount);
            assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.PG_CARD);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getCardType()).isEqualTo(cardType);
            assertThat(payment.getCardNo()).isEqualTo(cardNo);
            assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
            assertThat(payment.isPending()).isTrue();
        }

        @DisplayName("콜백 URL이 null이거나 빈 값이면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void throwsException_whenCallbackUrlIsBlank(String callbackUrl) {
            assertThatThrownBy(() -> Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L, callbackUrl, "key-123"
            ))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("콜백 URL은 필수")
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("결제 상태 변경")
    class PaymentStatusChange {

        @DisplayName("결제를 성공 상태로 변경할 수 있다.")
        @Test
        void markAsSuccess() {
            // given
            Payment payment = Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L,
                    "http://callback.url", "key-123"
            );

            // when
            payment.markAsSuccess();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.isSuccess()).isTrue();
            assertThat(payment.isFailed()).isFalse();
        }

        @DisplayName("결제를 실패 상태로 변경할 수 있다.")
        @Test
        void markAsFailed() {
            // given
            Payment payment = Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L,
                    "http://callback.url", "key-123"
            );
            String failureReason = "포인트 부족";

            // when
            payment.markAsFailed(failureReason);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo(failureReason);
            assertThat(payment.isFailed()).isTrue();
        }

        @DisplayName("결제를 카드 한도 초과 상태로 변경할 수 있다.")
        @Test
        void markAsLimitExceeded() {
            // given
            Payment payment = Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L,
                    "http://callback.url", "key-123"
            );

            // when
            payment.markAsLimitExceeded();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.LIMIT_EXCEEDED);
            assertThat(payment.getFailureReason()).isEqualTo("카드 한도 초과");
            assertThat(payment.isFailed()).isTrue();
        }

        @DisplayName("결제를 잘못된 카드 상태로 변경할 수 있다.")
        @Test
        void markAsInvalidCard() {
            // given
            Payment payment = Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L,
                    "http://callback.url", "key-123"
            );

            // when
            payment.markAsInvalidCard();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INVALID_CARD);
            assertThat(payment.getFailureReason()).isEqualTo("잘못된 카드 정보");
            assertThat(payment.isFailed()).isTrue();
        }

        @DisplayName("결제를 PG 타임아웃 상태로 변경할 수 있다.")
        @Test
        void markAsPgTimeout() {
            // given
            Payment payment = Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L,
                    "http://callback.url", "key-123"
            );

            // when
            payment.markAsTimeout();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PG_TIMEOUT);
            assertThat(payment.getFailureReason()).isEqualTo("PG 결제 요청 시간 초과");
            assertThat(payment.isFailed()).isTrue();
        }

        @DisplayName("결제를 콜백 타임아웃 상태로 변경할 수 있다.")
        @Test
        void markAsCallbackTimeout() {
            // given
            Payment payment = Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L,
                    "http://callback.url", "key-123"
            );

            // when
            payment.markAsCallbackTimeout();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CALLBACK_TIMEOUT);
            assertThat(payment.getFailureReason()).isEqualTo("결제 콜백 미수신 (30초 초과)");
            assertThat(payment.isFailed()).isTrue();
        }

        @DisplayName("이미 성공한 결제를 다시 성공 처리해도 상태가 유지된다.")
        @Test
        void markAsSuccess_whenAlreadySuccess() {
            // given
            Payment payment = Payment.createForPoint(1L, 100L, 1000L, "key-123");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            // when
            payment.markAsSuccess();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    @Nested
    @DisplayName("거래 ID 업데이트")
    class UpdateTransactionId {

        @DisplayName("거래 ID를 업데이트할 수 있다.")
        @Test
        void updateTransactionId() {
            // given
            Payment payment = Payment.createForPgCard(
                    1L, 100L, "SAMSUNG", "1234-5678", 1000L,
                    "http://callback.url", "key-123"
            );
            String transactionId = "20250105:TR:abc123";

            // when
            payment.updateTransactionId(transactionId);

            // then
            assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        }
    }
}
