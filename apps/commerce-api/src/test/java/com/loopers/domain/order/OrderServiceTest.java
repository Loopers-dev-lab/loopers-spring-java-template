package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.quantity.Quantity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private OrderService orderService;

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
          OrderItem.of(100L, "상품1", Quantity.of(2L), OrderPrice.of(10000L)),
          OrderItem.of(200L, "상품2", Quantity.of(1L), OrderPrice.of(30000L))
      );
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      given(orderRepository.save(any(Order.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      orderService.create(userId, orderItems, orderedAt);

      // then
      then(orderRepository).should(times(1)).save(orderCaptor.capture());
      Order capturedOrder = orderCaptor.getValue();

      assertThat(capturedOrder)
          .extracting("userId", "status", "orderedAt")
          .containsExactly(userId, OrderStatus.PENDING, orderedAt);
      assertThat(capturedOrder.getTotalAmountValue()).isEqualTo(50000L);

      assertThat(capturedOrder.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("주문 생성 시 OrderItem이 Order에 추가되고 양방향 관계가 설정된다")
    void createOrder_bidirectionalRelationship() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L))
      );

      given(orderRepository.save(any(Order.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);

      // then
      then(orderRepository).should(times(1)).save(orderCaptor.capture());
      Order capturedOrder = orderCaptor.getValue();

      assertThat(capturedOrder.getItems()).hasSize(1);

      OrderItem capturedItem = capturedOrder.getItems().get(0);
      assertThat(capturedItem.getOrder()).isEqualTo(capturedOrder);

      assertThat(capturedItem)
          .extracting("productId", "productName")
          .containsExactly(100L, "상품1");
      assertThat(capturedItem.getQuantityValue()).isEqualTo(1L);
      assertThat(capturedItem.getOrderPriceValue()).isEqualTo(10000L);
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
      Optional<Order> result = orderService.getById(orderId);

      // then
      assertThat(result).isPresent();
      assertThat(result.get())
          .extracting("userId", "status")
          .containsExactly(1L, OrderStatus.COMPLETED);
      then(orderRepository).should(times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 ID로 조회 시 해당 주문이 존재하지 않으면 Optional.empty()가 반환된다")
    void getById_notFound() {
      // given
      Long orderId = 999L;
      given(orderRepository.findById(orderId)).willReturn(Optional.empty());

      // when
      Optional<Order> result = orderService.getById(orderId);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주문 ID로 조회 시 @EntityGraph를 사용하여 OrderItem과 함께 조회된다")
    void getWithItemsById_success() {
      // given
      Long orderId = 1L;
      Order order = Order.of(1L, OrderStatus.COMPLETED, 50000L, ORDERED_AT_2025_10_30);
      given(orderRepository.findWithItemsById(orderId)).willReturn(Optional.of(order));

      // when
      Optional<Order> result = orderService.getWithItemsById(orderId);

      // then
      assertThat(result).isPresent();
      then(orderRepository).should(times(1)).findWithItemsById(orderId);
    }
  }

  @Nested
  @DisplayName("주문 완료")
  class CompleteOrder {

    @Test
    @DisplayName("PENDING 상태의 주문을 완료할 수 있다")
    void shouldCompleteOrder_whenPending() {
      // given
      Long orderId = 1L;
      Order order = Order.of(1L, OrderStatus.PENDING, 50000L, ORDERED_AT_2025_10_30);
      given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
      given(orderRepository.save(order)).willReturn(order);

      // when
      Order result = orderService.completeOrder(orderId);

      // then
      assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
      then(orderRepository).should(times(1)).findById(orderId);
      then(orderRepository).should(times(1)).save(order);
    }
  }

  @Nested
  @DisplayName("재시도 후 주문 완료")
  class RetryCompleteOrder {

    @Test
    @DisplayName("PAYMENT_FAILED 상태의 주문을 재시도 완료할 수 있다")
    void shouldRetryCompleteOrder_whenPaymentFailed() {
      // given
      Long orderId = 1L;
      Order order = Order.of(1L, OrderStatus.PAYMENT_FAILED, 50000L, ORDERED_AT_2025_10_30);
      given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
      given(orderRepository.save(order)).willReturn(order);

      // when
      Order result = orderService.retryCompleteOrder(orderId);

      // then
      assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
      then(orderRepository).should(times(1)).findById(orderId);
      then(orderRepository).should(times(1)).save(order);
    }
  }
}
