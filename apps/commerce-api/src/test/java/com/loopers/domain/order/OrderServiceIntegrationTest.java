package com.loopers.domain.order;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.quantity.Quantity;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("OrderService 통합 테스트")
class OrderServiceIntegrationTest {

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  private static final LocalDateTime ORDERED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("주문 생성 및 조회")
  class CreateAndGetOrder {

    @Test
    @DisplayName("주문을 생성한 뒤 ID로 조회하면 생성된 주문 정보가 정확히 반환된다")
    void createOrder_thenGet_dataMatches() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(2), OrderPrice.of(10000L)),
          OrderItem.of(200L, "상품2", Quantity.of(1), OrderPrice.of(30000L))
      );

      // when
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      Order foundOrder = orderService.getById(savedOrder.getId());

      // then
      assertThat(foundOrder)
          .extracting("userId", "status", "orderedAt")
          .containsExactly(userId, OrderStatus.PAYMENT_PENDING, ORDERED_AT_2025_10_30);
      assertThat(foundOrder.getTotalAmountValue()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("주문 생성 시 OrderItem이 cascade로 함께 저장되어 조회된다")
    void createOrder_itemsCascadePersisted() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1), OrderPrice.of(10000L)),
          OrderItem.of(200L, "상품2", Quantity.of(2), OrderPrice.of(20000L))
      );

      // when
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      Order foundOrder = orderService.getWithItemsById(savedOrder.getId());

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
  @DisplayName("주문 상태 변경")
  class UpdateOrderStatus {

    @Test
    @DisplayName("주문 상태를 PAYMENT_PENDING에서 COMPLETED로 변경하면 변경된 상태가 조회된다")
    void updateOrderStatus_thenGet_statusChanged() {
      // given
      Long userId = 1L;
      List<OrderItem> orderItems = List.of(
          OrderItem.of(100L, "상품1", Quantity.of(1), OrderPrice.of(10000L))
      );
      Order savedOrder = orderService.create(userId, orderItems, ORDERED_AT_2025_10_30);
      assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

      // when
      orderService.updateOrderStatus(savedOrder.getId(), OrderStatus.COMPLETED);
      Order foundOrder = orderService.getById(savedOrder.getId());

      // then
      assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
  }
}
