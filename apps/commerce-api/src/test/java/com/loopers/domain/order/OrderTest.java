package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * packageName : com.loopers.domain.order
 * fileName     : OrderTest
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
class OrderTest {

    @Nested
    @DisplayName("Order 생성 테스트")
    class CreateOrderTest {

        @Test
        @DisplayName("주문 생성 성공")
        void createOrderSuccess() {
            // when
            Order order = Order.create("user123");

            // then
            assertThat(order.getUserId()).isEqualTo("user123");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getTotalAmount()).isEqualTo(0L);
            assertThat(order.getCreatedAt()).isNotNull();
            assertThat(order.getOrderItems()).isEmpty();
        }

        @Test
        @DisplayName("userId가 null이면 생성 실패")
        void createOrderFailUserIdNull() {
            assertThatThrownBy(() -> Order.create(null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용자 ID는 필수");
        }

        @Test
        @DisplayName("userId가 공백이면 생성 실패")
        void createOrderFailUserIdBlank() {
            assertThatThrownBy(() -> Order.create(""))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용자 ID는 필수");
        }
    }

    @Nested
    @DisplayName("Order 상태 업데이트 테스트")
    class UpdateStatusTest {

        @Test
        @DisplayName("주문 상태 업데이트 성공")
        void updateStatusSuccess() {
            // given
            Order order = Order.create("user123");

            // when
            order.updateStatus(OrderStatus.COMPLETE);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETE);
        }
    }

    @Nested
    @DisplayName("총액 업데이트 테스트")
    class UpdateAmountTest {

        @Test
        @DisplayName("총액 업데이트 성공")
        void updateTotalAmountSuccess() {
            // given
            Order order = Order.create("user123");

            // when
            order.updateTotalAmount(5000L);

            // then
            assertThat(order.getTotalAmount()).isEqualTo(5000L);
        }
    }

    @Nested
    @DisplayName("OrderItem 추가 테스트")
    class AddOrderItemTest {

        @Test
        @DisplayName("OrderItem 추가 성공")
        void addOrderItemSuccess() {
            // given
            Order order = Order.create("user123");

            OrderItem item = OrderItem.create(
                    1L,
                    "상품명",
                    2L,
                    1000L
            );

            // when
            order.addOrderItem(item);
            item.setOrder(order);

            // then
            assertThat(order.getOrderItems()).hasSize(1);
            assertThat(order.getOrderItems().getFirst().getProductName()).isEqualTo("상품명");
            assertThat(item.getOrder()).isEqualTo(order);
        }
    }
}
