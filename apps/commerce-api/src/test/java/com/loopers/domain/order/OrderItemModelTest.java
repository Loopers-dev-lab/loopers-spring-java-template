package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderItemModelTest {
  OrderItem orderItems;

  @DisplayName("주문상세 모델을 생성할 때, ")
  @Nested
  class Create_Order {
    @DisplayName("사용자ID, 상태, 지불금액,총금액, 주문일자가 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Order_객체생성() {
      orderItems = OrderItem.create(1L, 2L, Money.wons(5_000));
      assertThat(orderItems.getRefProductId()).isEqualTo(1L);
      assertThat(orderItems.getQuantity()).isEqualTo(2L);
      assertThat(orderItems.getUnitPrice()).isEqualTo(Money.wons(5_000));

    }
  }

  @DisplayName("주문 모델을 생성할 때, 검증")
  @Nested
  class Valid_Order {

    @Test
    void 실패_수량_음수오류() {
      Money unitPrice = Money.wons(5_000);
      assertThrows(CoreException.class, () -> {
        orderItems = OrderItem.create(1L, -2L, unitPrice);
      });
    }

    @Test
    void 실패_단가_음수오류() {
      assertThrows(IllegalArgumentException.class, () -> {
        orderItems = OrderItem.create(1L, 2L, Money.wons(-5_000));
      });
    }
  }
}
