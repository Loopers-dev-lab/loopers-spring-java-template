package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.quantity.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private OrderService sut;

  @Captor
  private ArgumentCaptor<Order> orderCaptor;

  private static final LocalDateTime ORDERED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @Nested
  @DisplayName("주문 생성")
  class CreateOrder {

    @Test
    @DisplayName("사용자 ID와 주문 항목 목록으로 주문을 생성하면 총액이 계산되고 OrderItem이 추가된다")
    void createOrder_success() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(2), OrderPrice.of(10000L)),
          OrderItem.of(200L, "상품2", Quantity.of(1), OrderPrice.of(30000L))
      );
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      Order savedOrder = Order.of(userId, OrderStatus.PAYMENT_PENDING, 50000L, orderedAt);
      given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

      // when
      Order result = sut.create(userId, orderItems, orderedAt);

      // then
      then(orderRepository).should(times(1)).save(orderCaptor.capture());
      Order capturedOrder = orderCaptor.getValue();

      assertThat(result)
          .extracting("userId", "status", "orderedAt")
          .containsExactly(userId, OrderStatus.PAYMENT_PENDING, orderedAt);
      assertThat(result.getTotalAmountValue()).isEqualTo(50000L);

      assertThat(capturedOrder)
          .extracting("userId", "status")
          .containsExactly(userId, OrderStatus.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("주문 생성 시 OrderItem이 Order에 추가되고 양방향 관계가 설정된다")
    void createOrder_bidirectionalRelationship() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1), OrderPrice.of(10000L))
      );

      Order savedOrder = Order.of(userId, OrderStatus.PAYMENT_PENDING, 10000L, ORDERED_AT_2025_10_30);
      OrderItem item = OrderItem.of(100L, "상품1", Quantity.of(1), OrderPrice.of(10000L));
      savedOrder.addItem(item);

      given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

      // when
      Order result = sut.create(userId, orderItems, ORDERED_AT_2025_10_30);

      // then
      assertThat(result.getItems()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("주문 조회")
  class GetOrder {

    @Test
    @DisplayName("주문 ID로 조회 시 해당 주문이 존재하면 주문 정보가 반환된다")
    void getById_success() {
      // given
      Long orderId = 1L;
      Order order = Order.of(1L, OrderStatus.COMPLETED, 50000L, ORDERED_AT_2025_10_30);
      given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

      // when
      Order result = sut.getById(orderId);

      // then
      assertThat(result)
          .extracting("userId", "status")
          .containsExactly(1L, OrderStatus.COMPLETED);
      then(orderRepository).should(times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 ID로 조회 시 해당 주문이 존재하지 않으면 NOT_FOUND 예외가 발생한다")
    void getById_notFound() {
      // given
      Long orderId = 999L;
      given(orderRepository.findById(orderId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> sut.getById(orderId))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
          .hasMessageContaining("주문을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("주문 ID로 조회 시 @EntityGraph를 사용하여 OrderItem과 함께 조회된다")
    void getWithItemsById_success() {
      // given
      Long orderId = 1L;
      Order order = Order.of(1L, OrderStatus.COMPLETED, 50000L, ORDERED_AT_2025_10_30);
      given(orderRepository.findWithItemsById(orderId)).willReturn(Optional.of(order));

      // when
      Order result = sut.getWithItemsById(orderId);

      // then
      assertThat(result).isNotNull();
      then(orderRepository).should(times(1)).findWithItemsById(orderId);
    }
  }

  @Nested
  @DisplayName("주문 상태 업데이트")
  class UpdateOrderStatus {

    @Test
    @DisplayName("주문 ID와 새로운 상태로 상태를 업데이트하면 주문의 상태가 변경된다")
    void updateOrderStatus_success() {
      // given
      Long orderId = 1L;
      Order order = Order.of(1L, OrderStatus.PAYMENT_PENDING, 50000L, ORDERED_AT_2025_10_30);
      given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

      // when
      Order result = sut.updateOrderStatus(orderId, OrderStatus.COMPLETED);

      // then
      assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
      then(orderRepository).should(times(1)).findById(orderId);
    }
  }
}
