package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.quantity.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order 도메인 테스트")
class OrderTest {

  private static final LocalDateTime ORDERED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @DisplayName("Order를 생성할 때")
  @Nested
  class Create {

    @DisplayName("사용자 ID, 주문 상태, 총액, 주문 시각을 모두 제공하면 Order가 생성된다")
    @Test
    void shouldCreate_whenValid() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;
      Long pointUsedAmount = 20000L;
      Long pgAmount = 30000L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      Order order = Order.of(userId, status, totalAmount, pointUsedAmount, pgAmount, orderedAt);

      assertThat(order).extracting("userId", "status", "orderedAt")
          .containsExactly(userId, status, orderedAt);
      assertThat(order.getTotalAmountValue()).isEqualTo(totalAmount);
      assertThat(order.getPointUsedAmountValue()).isEqualTo(pointUsedAmount);
      assertThat(order.getPgAmountValue()).isEqualTo(pgAmount);
    }
  }

  @DisplayName("userId 검증")
  @Nested
  class ValidateUserId {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      assertThatThrownBy(() -> Order.of(null, status, totalAmount, 0L, totalAmount, orderedAt))
          .isInstanceOf(CoreException.class)
          .hasMessage("사용자는 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_USER_EMPTY);
    }
  }

  @DisplayName("status 검증")
  @Nested
  class ValidateStatus {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Long userId = 1L;
      Long totalAmount = 50000L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      assertThatThrownBy(() -> Order.of(userId, null, totalAmount, 0L, totalAmount, orderedAt))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 상태는 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_STATUS_EMPTY);
    }
  }

  @DisplayName("totalAmount 검증")
  @Nested
  class ValidateTotalAmount {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      assertThatThrownBy(() -> Order.of(userId, status, null, 0L, 50000L, orderedAt))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_MONEY_VALUE);
    }

    @DisplayName("음수이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegative() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      assertThatThrownBy(() -> Order.of(userId, status, -1000L, 0L, -1000L, orderedAt))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 음수일 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.NEGATIVE_MONEY_VALUE);
    }

    @DisplayName("0이면 정상 생성된다")
    @Test
    void shouldCreate_whenZero() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 0L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      Order order = Order.of(userId, status, totalAmount, 0L, totalAmount, orderedAt);

      assertThat(order.getTotalAmountValue()).isZero();
    }
  }

  @DisplayName("orderedAt 검증")
  @Nested
  class ValidateOrderedAt {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;

      assertThatThrownBy(() -> Order.of(userId, status, totalAmount, 0L, totalAmount, null))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 시각은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_ORDERED_AT_EMPTY);
    }
  }

  @DisplayName("pointUsedAmount 검증")
  @Nested
  class ValidatePointUsedAmount {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;

      assertThatThrownBy(() -> Order.of(userId, status, totalAmount, null, totalAmount, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_MONEY_VALUE);
    }

    @DisplayName("음수이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegative() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;

      assertThatThrownBy(() -> Order.of(userId, status, totalAmount, -1000L, totalAmount, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 음수일 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.NEGATIVE_MONEY_VALUE);
    }
  }

  @DisplayName("pgAmount 검증")
  @Nested
  class ValidatePgAmount {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;

      assertThatThrownBy(() -> Order.of(userId, status, totalAmount, 0L, null, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_MONEY_VALUE);
    }

    @DisplayName("음수이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNegative() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;

      assertThatThrownBy(() -> Order.of(userId, status, totalAmount, 0L, -1000L, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasMessage("금액은 음수일 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.NEGATIVE_MONEY_VALUE);
    }
  }

  @DisplayName("금액 불변식 검증 (totalAmount == pointUsedAmount + pgAmount)")
  @Nested
  class ValidateTotalAmountInvariant {

    @DisplayName("totalAmount와 pointUsedAmount + pgAmount가 같으면 정상 생성된다")
    @Test
    void shouldCreate_whenInvariantSatisfied() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;
      Long pointUsedAmount = 20000L;
      Long pgAmount = 30000L;

      Order order = Order.of(userId, status, totalAmount, pointUsedAmount, pgAmount, ORDERED_AT_2025_10_30);

      assertThat(order.getTotalAmountValue()).isEqualTo(totalAmount);
      assertThat(order.getPointUsedAmountValue()).isEqualTo(pointUsedAmount);
      assertThat(order.getPgAmountValue()).isEqualTo(pgAmount);
    }

    @DisplayName("totalAmount와 pointUsedAmount + pgAmount가 다르면 예외가 발생한다")
    @Test
    void shouldThrowException_whenInvariantViolated() {
      Long userId = 1L;
      OrderStatus status = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;
      Long pointUsedAmount = 20000L;
      Long pgAmount = 20000L;

      assertThatThrownBy(() -> Order.of(userId, status, totalAmount, pointUsedAmount, pgAmount, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 금액이 일치하지 않습니다. (totalAmount != pointUsedAmount + pgAmount)")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_AMOUNT_MISMATCH);
    }
  }

  @DisplayName("hasPgAmount 검증")
  @Nested
  class HasPgAmount {

    @DisplayName("pgAmount가 0보다 크면 true를 반환한다")
    @Test
    void shouldReturnTrue_whenPgAmountPositive() {
      Order order = Order.of(1L, OrderStatus.PENDING, 50000L, 0L, 50000L, ORDERED_AT_2025_10_30);

      assertThat(order.hasPgAmount()).isTrue();
    }

    @DisplayName("pgAmount가 0이면 false를 반환한다")
    @Test
    void shouldReturnFalse_whenPgAmountZero() {
      Order order = Order.of(1L, OrderStatus.PENDING, 50000L, 50000L, 0L, ORDERED_AT_2025_10_30);

      assertThat(order.hasPgAmount()).isFalse();
    }
  }

  @DisplayName("주문 완료")
  @Nested
  class Complete {

    @DisplayName("PENDING 상태에서 완료할 수 있다")
    @Test
    void shouldComplete_whenPending() {
      Order order = Order.of(1L, OrderStatus.PENDING, 50000L, 0L, 50000L, ORDERED_AT_2025_10_30);

      order.complete();

      assertThat(order).extracting("status").isEqualTo(OrderStatus.COMPLETED);
    }

    @DisplayName("PENDING이 아닌 상태에서 완료하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNotPending() {
      Order order = Order.of(1L, OrderStatus.COMPLETED, 50000L, 0L, 50000L, ORDERED_AT_2025_10_30);

      assertThatThrownBy(order::complete)
          .isInstanceOf(CoreException.class)
          .hasMessage("PENDING 상태의 주문만 완료할 수 있습니다.")
          .extracting("errorType").isEqualTo(ErrorType.ORDER_CANNOT_COMPLETE);
    }
  }

  @DisplayName("결제 실패 상태 변경")
  @Nested
  class FailPayment {

    @DisplayName("PENDING 상태에서 PAYMENT_FAILED로 변경할 수 있다")
    @Test
    void shouldFailPayment_whenPending() {
      Order order = Order.of(1L, OrderStatus.PENDING, 50000L, 0L, 50000L, ORDERED_AT_2025_10_30);

      order.failPayment();

      assertThat(order).extracting("status").isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @DisplayName("PENDING이 아닌 상태에서 변경하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNotPending() {
      Order order = Order.of(1L, OrderStatus.COMPLETED, 50000L, 0L, 50000L, ORDERED_AT_2025_10_30);

      assertThatThrownBy(order::failPayment)
          .isInstanceOf(CoreException.class)
          .hasMessage("PENDING 상태의 주문만 결제 실패 상태로 변경할 수 있습니다.")
          .extracting("errorType").isEqualTo(ErrorType.ORDER_CANNOT_FAIL_PAYMENT);
    }
  }

  @DisplayName("재시도 후 주문 완료")
  @Nested
  class RetryComplete {

    @DisplayName("PAYMENT_FAILED 상태에서 완료할 수 있다")
    @Test
    void shouldRetryComplete_whenPaymentFailed() {
      Order order = Order.of(1L, OrderStatus.PAYMENT_FAILED, 50000L, 0L, 50000L, ORDERED_AT_2025_10_30);

      order.retryComplete();

      assertThat(order).extracting("status").isEqualTo(OrderStatus.COMPLETED);
    }

    @DisplayName("PAYMENT_FAILED가 아닌 상태에서 완료하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNotPaymentFailed() {
      Order order = Order.of(1L, OrderStatus.PENDING, 50000L, 0L, 50000L, ORDERED_AT_2025_10_30);

      assertThatThrownBy(order::retryComplete)
          .isInstanceOf(CoreException.class)
          .hasMessage("PAYMENT_FAILED 상태의 주문만 재시도 완료할 수 있습니다.")
          .extracting("errorType").isEqualTo(ErrorType.ORDER_CANNOT_RETRY_COMPLETE);
    }
  }

  @DisplayName("Order-OrderItem 협업 테스트")
  @Nested
  class OrderWithOrderItem {

    @DisplayName("OrderItem을 추가하면 items에 포함된다")
    @Test
    void shouldAddItem() {
      // given
      Order order = Order.of(1L, OrderStatus.PENDING, 0L, 0L, 0L, ORDERED_AT_2025_10_30);
      OrderItem item = OrderItem.of(100L, "테스트 상품", Quantity.of(2L), OrderPrice.of(10000L));

      // when
      order.addItem(item);

      // then
      assertThat(order.getItems()).hasSize(1);
      assertThat(order.getItems()).contains(item);
    }

    @DisplayName("여러 OrderItem을 추가할 수 있다")
    @Test
    void shouldAddMultipleItems() {
      // given
      Order order = Order.of(1L, OrderStatus.PENDING, 0L, 0L, 0L, ORDERED_AT_2025_10_30);
      OrderItem item1 = OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L));
      OrderItem item2 = OrderItem.of(200L, "상품2", Quantity.of(2L), OrderPrice.of(20000L));

      // when
      order.addItem(item1);
      order.addItem(item2);

      // then
      assertThat(order.getItems()).hasSize(2);
      assertThat(order.getItems()).containsExactly(item1, item2);
    }

    @DisplayName("OrderItem 추가 시 양방향 관계가 설정된다")
    @Test
    void shouldSetBidirectionalRelationship() {
      // given
      Order order = Order.of(1L, OrderStatus.PENDING, 0L, 0L, 0L, ORDERED_AT_2025_10_30);
      OrderItem item = OrderItem.of(100L, "테스트 상품", Quantity.of(2L), OrderPrice.of(10000L));

      // when
      order.addItem(item);

      // then
      // OrderItem을 블랙박스로 취급하여 최소한의 검증만 수행
      assertThat(item.getOrder()).isEqualTo(order);
    }
  }
}
