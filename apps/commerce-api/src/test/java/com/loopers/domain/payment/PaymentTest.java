package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Payment 도메인 테스트")
class PaymentTest {

  private static final LocalDateTime REQUESTED_AT_2025_12_01 =
      LocalDateTime.of(2025, 12, 1, 10, 0, 0);

  @DisplayName("Payment 생성")
  @Nested
  class Create {

    @Test
    @DisplayName("올바른 정보로 Payment를 생성한다")
    void shouldCreate_whenValid() {
      Payment payment = Payment.of(
          1L,
          100L,
          CardType.SAMSUNG,
          "1234-5678-9012-3456",
          50000L,
          REQUESTED_AT_2025_12_01
      );

      assertThat(payment)
          .extracting("orderId", "userId", "cardType", "cardNo", "status")
          .containsExactly(1L, 100L, CardType.SAMSUNG, "1234-5678-9012-3456", PaymentStatus.REQUESTED);
      assertThat(payment.getAmountValue()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("orderId가 null이면 예외가 발생한다")
    void shouldThrowException_whenOrderIdIsNull() {
      assertThatThrownBy(() -> Payment.of(
          null,
          100L,
          CardType.SAMSUNG,
          "1234-5678-9012-3456",
          50000L,
          REQUESTED_AT_2025_12_01
      ))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_ORDER_EMPTY);
    }

    @Test
    @DisplayName("userId가 null이면 예외가 발생한다")
    void shouldThrowException_whenUserIdIsNull() {
      assertThatThrownBy(() -> Payment.of(
          1L,
          null,
          CardType.SAMSUNG,
          "1234-5678-9012-3456",
          50000L,
          REQUESTED_AT_2025_12_01
      ))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_USER_EMPTY);
    }

    @Test
    @DisplayName("cardType이 null이면 예외가 발생한다")
    void shouldThrowException_whenCardTypeIsNull() {
      assertThatThrownBy(() -> Payment.of(
          1L,
          100L,
          null,
          "1234-5678-9012-3456",
          50000L,
          REQUESTED_AT_2025_12_01
      ))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_CARD_TYPE_EMPTY);
    }

    @Test
    @DisplayName("cardNo가 null이면 예외가 발생한다")
    void shouldThrowException_whenCardNoIsNull() {
      assertThatThrownBy(() -> Payment.of(
          1L,
          100L,
          CardType.SAMSUNG,
          null,
          50000L,
          REQUESTED_AT_2025_12_01
      ))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_CARD_NO_EMPTY);
    }

    @Test
    @DisplayName("amount가 null이면 예외가 발생한다")
    void shouldThrowException_whenAmountIsNull() {
      assertThatThrownBy(() -> Payment.of(
          1L,
          100L,
          CardType.SAMSUNG,
          "1234-5678-9012-3456",
          null,
          REQUESTED_AT_2025_12_01
      ))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_AMOUNT_EMPTY);
    }

    @Test
    @DisplayName("amount가 0 이하이면 예외가 발생한다")
    void shouldThrowException_whenAmountIsNotPositive() {
      assertThatThrownBy(() -> Payment.of(
          1L,
          100L,
          CardType.SAMSUNG,
          "1234-5678-9012-3456",
          0L,
          REQUESTED_AT_2025_12_01
      ))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_PAYMENT_AMOUNT_NOT_POSITIVE);
    }
  }

  @DisplayName("상태 전이")
  @Nested
  class StateTransition {

    @Test
    @DisplayName("REQUESTED 상태에서 PENDING으로 전이한다")
    void shouldTransitionToPending_whenRequested() {
      Payment payment = createPayment();

      payment.toPending("tx-key-12345");

      assertThat(payment)
          .extracting("status", "transactionKey")
          .containsExactly(PaymentStatus.PENDING, "tx-key-12345");
    }

    @Test
    @DisplayName("REQUESTED 상태에서 REQUEST_FAILED로 전이한다")
    void shouldTransitionToRequestFailed_whenRequested() {
      Payment payment = createPayment();

      payment.toRequestFailed();

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUEST_FAILED);
    }

    @Test
    @DisplayName("PENDING 상태에서 SUCCESS로 전이한다")
    void shouldTransitionToSuccess_whenPending() {
      Payment payment = createPayment();
      payment.toPending("tx-key-12345");
      LocalDateTime completedAt = LocalDateTime.of(2025, 12, 1, 10, 5, 0);

      payment.toSuccess(completedAt);

      assertThat(payment)
          .extracting("status", "pgCompletedAt")
          .containsExactly(PaymentStatus.SUCCESS, completedAt);
    }

    @Test
    @DisplayName("PENDING 상태에서 FAILED로 전이한다")
    void shouldTransitionToFailed_whenPending() {
      Payment payment = createPayment();
      payment.toPending("tx-key-12345");
      LocalDateTime completedAt = LocalDateTime.of(2025, 12, 1, 10, 5, 0);

      payment.toFailed("LIMIT_EXCEEDED", completedAt);

      assertThat(payment)
          .extracting("status", "failureReason", "pgCompletedAt")
          .containsExactly(PaymentStatus.FAILED, "LIMIT_EXCEEDED", completedAt);
    }

    @Test
    @DisplayName("이미 완료된 상태에서 toSuccess 호출 시 예외가 발생한다")
    void shouldThrowException_whenToSuccessOnTerminalState() {
      Payment payment = createPayment();
      payment.toPending("tx-key-12345");
      payment.toSuccess(LocalDateTime.of(2025, 12, 1, 10, 5, 0));

      assertThatThrownBy(() -> payment.toSuccess(LocalDateTime.of(2025, 12, 1, 10, 10, 0)))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.PAYMENT_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("이미 완료된 상태에서 toFailed 호출 시 예외가 발생한다")
    void shouldThrowException_whenToFailedOnTerminalState() {
      Payment payment = createPayment();
      payment.toPending("tx-key-12345");
      payment.toSuccess(LocalDateTime.of(2025, 12, 1, 10, 5, 0));

      assertThatThrownBy(() -> payment.toFailed("ERROR", LocalDateTime.of(2025, 12, 1, 10, 10, 0)))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.PAYMENT_ALREADY_COMPLETED);
    }
  }

  @DisplayName("상태 확인 메서드")
  @Nested
  class StatusCheck {

    @Test
    @DisplayName("완료 상태를 정확히 판단한다")
    void shouldCheckCompletedStatus() {
      Payment successPayment = createPayment();
      successPayment.toPending("tx-1");
      successPayment.toSuccess(LocalDateTime.of(2025, 12, 1, 10, 5, 0));

      Payment failedPayment = createPayment();
      failedPayment.toPending("tx-2");
      failedPayment.toFailed("ERROR", LocalDateTime.of(2025, 12, 1, 10, 5, 0));

      Payment requestFailedPayment = createPayment();
      requestFailedPayment.toRequestFailed();

      Payment pendingPayment = createPayment();
      pendingPayment.toPending("tx-3");

      assertThat(successPayment.isCompleted()).isTrue();
      assertThat(failedPayment.isCompleted()).isTrue();
      assertThat(requestFailedPayment.isCompleted()).isTrue();
      assertThat(pendingPayment.isCompleted()).isFalse();
    }
  }

  private Payment createPayment() {
    return Payment.of(
        1L,
        100L,
        CardType.SAMSUNG,
        "1234-5678-9012-3456",
        50000L,
        REQUESTED_AT_2025_12_01
    );
  }
}