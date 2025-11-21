package com.loopers.domain.order;

import com.loopers.support.test.IntegrationTestSupport;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.quantity.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import com.loopers.support.error.CoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderService 통합 테스트")
class OrderServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private OrderService orderService;

  private static final LocalDateTime ORDERED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @Nested
  @DisplayName("주문 생성 및 조회")
  class CreateAndGetOrder {

    @Test
    @DisplayName("주문을 생성한 뒤 ID로 조회하면 생성된 주문 정보가 정확히 반환된다")
    void createOrder_thenGet_dataMatches() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(2L), OrderPrice.of(10000L)),
          OrderItem.of(200L, "상품2", Quantity.of(1L), OrderPrice.of(30000L))
      );

      // when
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      Order foundOrder = orderService.getById(savedOrder.getId()).orElseThrow();

      // then
      assertThat(foundOrder)
          .extracting("userId", "status", "orderedAt")
          .containsExactly(userId, OrderStatus.PENDING, ORDERED_AT_2025_10_30);
      assertThat(foundOrder.getTotalAmountValue()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("주문 생성 시 OrderItem이 함께 저장되고 조회된다")
    void createOrder_itemsCascadePersisted() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L)),
          OrderItem.of(200L, "상품2", Quantity.of(2L), OrderPrice.of(20000L))
      );

      // when
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      Order foundOrder = orderService.getWithItemsById(savedOrder.getId()).orElseThrow();

      // then
      assertThat(foundOrder.getItems())
          .hasSize(2)
          .extracting("productId", "productName")
          .containsExactlyInAnyOrder(
              org.assertj.core.groups.Tuple.tuple(100L, "상품1"),
              org.assertj.core.groups.Tuple.tuple(200L, "상품2")
          );
    }
  }

  @Nested
  @DisplayName("주문 완료 (PENDING → COMPLETED)")
  class CompleteOrder {

    @Test
    @DisplayName("COMPLETED 주문을 완료 시도하면 예외가 발생한다")
    void shouldThrowException_whenCompletedOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      orderService.completeOrder(savedOrder.getId());

      // when & then
      assertThatThrownBy(() -> orderService.completeOrder(savedOrder.getId()))
          .isInstanceOf(CoreException.class)
          .hasMessage("PENDING 상태의 주문만 완료할 수 있습니다.");
    }

    @Test
    @DisplayName("PAYMENT_FAILED 주문을 완료 시도하면 예외가 발생한다")
    void shouldThrowException_whenPaymentFailedOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      orderService.failPaymentOrder(savedOrder.getId());

      // when & then
      assertThatThrownBy(() -> orderService.completeOrder(savedOrder.getId()))
          .isInstanceOf(CoreException.class)
          .hasMessage("PENDING 상태의 주문만 완료할 수 있습니다.");
    }

    @Test
    @DisplayName("PENDING 주문을 완료하면 COMPLETED 상태로 변경된다")
    void shouldChangeToCompleted_whenPendingOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);

      // when
      orderService.completeOrder(savedOrder.getId());
      Order foundOrder = orderService.getById(savedOrder.getId()).orElseThrow();

      // then
      assertThat(foundOrder).extracting("status").isEqualTo(OrderStatus.COMPLETED);
    }
  }

  @Nested
  @DisplayName("결제 실패 (PENDING → PAYMENT_FAILED)")
  class FailPayment {

    @Test
    @DisplayName("COMPLETED 주문을 결제 실패 처리하면 예외가 발생한다")
    void shouldThrowException_whenCompletedOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      orderService.completeOrder(savedOrder.getId());

      // when & then
      assertThatThrownBy(() -> orderService.failPaymentOrder(savedOrder.getId()))
          .isInstanceOf(CoreException.class)
          .hasMessage("PENDING 상태의 주문만 결제 실패 상태로 변경할 수 있습니다.");
    }

    @Test
    @DisplayName("PENDING 주문의 결제가 실패하면 PAYMENT_FAILED 상태로 변경된다")
    void shouldChangeToPaymentFailed_whenPendingOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);

      // when
      orderService.failPaymentOrder(savedOrder.getId());
      Order foundOrder = orderService.getById(savedOrder.getId()).orElseThrow();

      // then
      assertThat(foundOrder).extracting("status").isEqualTo(OrderStatus.PAYMENT_FAILED);
    }
  }

  @Nested
  @DisplayName("재시도 완료 (PAYMENT_FAILED → COMPLETED)")
  class RetryComplete {

    @Test
    @DisplayName("PENDING 주문을 재시도 완료하면 예외가 발생한다")
    void shouldThrowException_whenPendingOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);

      // when & then
      assertThatThrownBy(() -> orderService.retryCompleteOrder(savedOrder.getId()))
          .isInstanceOf(CoreException.class)
          .hasMessage("PAYMENT_FAILED 상태의 주문만 재시도 완료할 수 있습니다.");
    }

    @Test
    @DisplayName("COMPLETED 주문을 재시도 완료하면 예외가 발생한다")
    void shouldThrowException_whenCompletedOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      orderService.completeOrder(savedOrder.getId());

      // when & then
      assertThatThrownBy(() -> orderService.retryCompleteOrder(savedOrder.getId()))
          .isInstanceOf(CoreException.class)
          .hasMessage("PAYMENT_FAILED 상태의 주문만 재시도 완료할 수 있습니다.");
    }

    @Test
    @DisplayName("PAYMENT_FAILED 주문을 재시도 완료하면 COMPLETED 상태로 변경된다")
    void shouldChangeToCompleted_whenPaymentFailedOrder() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      orderService.failPaymentOrder(savedOrder.getId());

      // when
      orderService.retryCompleteOrder(savedOrder.getId());
      Order foundOrder = orderService.getById(savedOrder.getId()).orElseThrow();

      // then
      assertThat(foundOrder).extracting("status").isEqualTo(OrderStatus.COMPLETED);
    }
  }
}
