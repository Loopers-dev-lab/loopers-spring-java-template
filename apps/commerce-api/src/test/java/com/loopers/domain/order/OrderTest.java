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
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      Order order = Order.of(userId, status, totalAmount, orderedAt);

      assertThat(order).extracting("userId", "status", "orderedAt")
          .containsExactly(userId, status, orderedAt);
      assertThat(order.getTotalAmountValue()).isEqualTo(totalAmount);
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

      assertThatThrownBy(() -> Order.of(null, status, totalAmount, orderedAt))
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

      assertThatThrownBy(() -> Order.of(userId, null, totalAmount, orderedAt))
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

      assertThatThrownBy(() -> Order.of(userId, status, null, orderedAt))
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

      assertThatThrownBy(() -> Order.of(userId, status, -1000L, orderedAt))
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

      Order order = Order.of(userId, status, totalAmount, orderedAt);

      assertThat(order.getTotalAmountValue()).isEqualTo(0L);
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

      assertThatThrownBy(() -> Order.of(userId, status, totalAmount, null))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 시각은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_ORDERED_AT_EMPTY);
    }
  }

  @DisplayName("비즈니스 로직")
  @Nested
  class BusinessLogic {

    @DisplayName("주문 상태를 COMPLETED에서 PAYMENT_PENDING으로 변경할 수 있다")
    @Test
    void shouldUpdateStatus() {
      Long userId = 1L;
      OrderStatus initialStatus = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;
      Order order = Order.of(userId, initialStatus, totalAmount, orderedAt);

      order.updateStatus(OrderStatus.PAYMENT_PENDING);

      assertThat(order).extracting("status").isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @DisplayName("상태 변경 시 null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenUpdateStatusWithNull() {
      Long userId = 1L;
      OrderStatus initialStatus = OrderStatus.COMPLETED;
      Long totalAmount = 50000L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;
      Order order = Order.of(userId, initialStatus, totalAmount, orderedAt);

      assertThatThrownBy(() -> order.updateStatus(null))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 상태는 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_STATUS_EMPTY);
    }
  }

  @DisplayName("Order-OrderItem 협업 테스트")
  @Nested
  class OrderWithOrderItem {

    @DisplayName("OrderItem을 추가하면 items에 포함된다")
    @Test
    void shouldAddItem() {
      // given
      Order order = Order.of(1L, OrderStatus.PENDING, 0L, ORDERED_AT_2025_10_30);
      OrderItem item = OrderItem.of(100L, "테스트 상품", Quantity.of(2), OrderPrice.of(10000L));

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
      Order order = Order.of(1L, OrderStatus.PENDING, 0L, ORDERED_AT_2025_10_30);
      OrderItem item1 = OrderItem.of(100L, "상품1", Quantity.of(1), OrderPrice.of(10000L));
      OrderItem item2 = OrderItem.of(200L, "상품2", Quantity.of(2), OrderPrice.of(20000L));

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
      Order order = Order.of(1L, OrderStatus.PENDING, 0L, ORDERED_AT_2025_10_30);
      OrderItem item = OrderItem.of(100L, "테스트 상품", Quantity.of(2), OrderPrice.of(10000L));

      // when
      order.addItem(item);

      // then
      // OrderItem을 블랙박스로 취급하여 최소한의 검증만 수행
      assertThat(item.getOrder()).isEqualTo(order);
    }
  }
}
