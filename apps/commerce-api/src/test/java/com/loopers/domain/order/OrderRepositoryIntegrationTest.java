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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Transactional
@DisplayName("OrderRepository 통합 테스트")
class OrderRepositoryIntegrationTest {

  private static final LocalDateTime ORDERED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("주문 목록 조회 (@Query + DTO)")
  @Nested
  class FindOrderList {

    @DisplayName("사용자 ID로 주문을 페이지네이션 조회하면 DTO 목록이 반환된다")
    @Test
    void shouldReturnDtoPage() {
      // given
      Long userId = 1L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      Order order1 = Order.of(userId, OrderStatus.COMPLETED, 30000L, orderedAt);
      order1.addItem(OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L)));
      order1.addItem(OrderItem.of(200L, "상품2", Quantity.of(2L), OrderPrice.of(10000L)));
      orderRepository.save(order1);

      Order order2 = Order.of(userId, OrderStatus.PENDING, 50000L, orderedAt.plusDays(1));
      order2.addItem(OrderItem.of(300L, "상품3", Quantity.of(5L), OrderPrice.of(10000L)));
      orderRepository.save(order2);

      PageRequest pageRequest = PageRequest.of(0, 10);

      // when
      Page<OrderListDto> result = orderRepository.findOrderList(userId, pageRequest);

      // then
      assertThat(result.getContent())
          .hasSize(2)
          .extracting("userId", "status", "itemCount", "totalAmount")
          .containsExactly(
              tuple(userId, OrderStatus.COMPLETED, 2, 30000L),
              tuple(userId, OrderStatus.PENDING, 1, 50000L)
          );
    }

    @DisplayName("다른 사용자의 주문은 조회되지 않는다")
    @Test
    void shouldFilterByUserId() {
      // given
      Long userId1 = 1L;
      Long userId2 = 2L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      Order order1 = Order.of(userId1, OrderStatus.COMPLETED, 10000L, orderedAt);
      order1.addItem(OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L)));
      orderRepository.save(order1);

      Order order2 = Order.of(userId2, OrderStatus.PENDING, 20000L, orderedAt);
      order2.addItem(OrderItem.of(200L, "상품2", Quantity.of(2L), OrderPrice.of(10000L)));
      orderRepository.save(order2);

      PageRequest pageRequest = PageRequest.of(0, 10);

      // when
      Page<OrderListDto> result = orderRepository.findOrderList(userId1, pageRequest);

      // then
      assertThat(result.getContent())
          .hasSize(1)
          .element(0).extracting("userId")
          .isEqualTo(userId1);
    }

    @DisplayName("15개 주문 중 두 번째 페이지(size=10)를 조회하면 5개가 반환된다")
    @Test
    void shouldPaginateCorrectly() {
      // given
      Long userId = 1L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      for (int i = 0; i < 15; i++) {
        Order order = Order.of(userId, OrderStatus.COMPLETED, 10000L, orderedAt.plusDays(i));
        order.addItem(OrderItem.of(100L + i, "상품" + i, Quantity.of(1L), OrderPrice.of(10000L)));
        orderRepository.save(order);
      }

      PageRequest pageRequest = PageRequest.of(1, 10);

      // when
      Page<OrderListDto> result = orderRepository.findOrderList(userId, pageRequest);

      // then
      assertAll(
          () -> assertThat(result.getContent()).hasSize(5),
          () -> assertThat(result.getTotalElements()).isEqualTo(15L),
          () -> assertThat(result.getTotalPages()).isEqualTo(2)
      );
    }
  }

  @DisplayName("주문 상세 조회 (@EntityGraph)")
  @Nested
  class FindWithItemsById {

    @DisplayName("OrderItem을 포함하여 조회하면 함께 조회된다 (N+1 방지)")
    @Test
    void shouldFetchWithItems() {
      // given
      Long userId = 1L;
      LocalDateTime orderedAt = ORDERED_AT_2025_10_30;

      Order order = Order.of(userId, OrderStatus.COMPLETED, 30000L, orderedAt);
      order.addItem(OrderItem.of(100L, "상품1", Quantity.of(1L), OrderPrice.of(10000L)));
      order.addItem(OrderItem.of(200L, "상품2", Quantity.of(2L), OrderPrice.of(10000L)));
      Order savedOrder = orderRepository.save(order);

      // when
      Optional<Order> result = orderRepository.findWithItemsById(savedOrder.getId());

      // then
      assertThat(result).isPresent();
      assertThat(result.get().getItems())
          .hasSize(2)
          .extracting("productName")
          .containsExactly("상품1", "상품2");
    }

    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional이 반환된다")
    @Test
    void shouldReturnEmpty_whenNotFound() {
      // given
      Long nonExistentId = 999L;

      // when
      Optional<Order> result = orderRepository.findWithItemsById(nonExistentId);

      // then
      assertThat(result).isEmpty();
    }
  }
}
