package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderModelTest {
  Order order;
  List<OrderItem> orderItems = new ArrayList<>();

  @BeforeEach
  void setup() {
    orderItems = new ArrayList<>();
    orderItems.add(OrderItem.create(1L, 2L, Money.wons(5_000)));
  }

  @DisplayName("주문 모델을 생성할 때, ")
  @Nested
  class Create_Order {
    @DisplayName("사용자ID, 상태, 지불금액,총금액, 주문일자가 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Order_객체생성() {
      order = Order.create(1, orderItems);
      assertThat(order.getRefUserId()).isEqualTo(1);
      assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

    }
  }

  @DisplayName("주문 확인시, 상품준비중 상태로 변경")
  @Nested
  class Valid_상품준비중 {
    @Test
    void 실패_주문확인_결제전오류() {
      order = Order.create(1, orderItems);
      assertThatThrownBy(() -> {
        order.preparing();
      }).isInstanceOf(CoreException.class).hasMessageContaining("결제대기중");
    }

    @Test
    void 실패_주문확인_상품준비중오류() {
      order = Order.create(1, orderItems);
      order.paid();
      order.preparing();
      assertThatThrownBy(() -> {
        order.preparing();
      }).isInstanceOf(CoreException.class).hasMessageContaining("이미 준비중");
    }


    @Test
    void 성공_상품준비중() {
      order = Order.create(1, orderItems);
      order.paid();
      order.preparing();
      assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }
  }

  @DisplayName("상품 모델을 생성후, 주문취소")
  @Nested
  class Valid_주문취소 {
    @Test
    void 실패_주문취소_이미취소된주문오류() {
      order = Order.create(1, orderItems);
      order.cancel();
      assertThrows(CoreException.class, () -> {
        order.cancel();
      });
    }

    @Test
    void 성공_주문취소() {
      order = Order.create(1, orderItems);
      order.cancel();
      assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
  }
}
