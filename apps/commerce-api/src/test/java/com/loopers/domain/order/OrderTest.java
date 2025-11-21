package com.loopers.domain.order;

import com.loopers.domain.common.vo.Money;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {

        @DisplayName("정상적인 주문이 성공적으로 생성된다")
        @Test
        void shouldCreateOrder_whenValidInput() {
            List<OrderItem> items = List.of(
                    new OrderItem(1L, 2, Money.of(10000)),
                    new OrderItem(2L, 1, Money.of(15000))
            );
            
            Order order = Order.create("member1", items);
            
            assertThat(order.getMemberId()).isEqualTo("member1");
            assertThat(order.getItems()).hasSize(2);
            assertThat(order.getTotalPrice()).isEqualTo(Money.of(35000)); // 20000 + 15000
        }

        @DisplayName("회원 ID가 null이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenMemberIdIsNull() {
            List<OrderItem> items = List.of(
                    new OrderItem(1L, 1, Money.of(10000))
            );
            
            assertThatThrownBy(() -> Order.create(null, items))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("회원 ID는 필수입니다");
        }

        @DisplayName("회원 ID가 빈값이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenMemberIdIsEmpty() {
            List<OrderItem> items = List.of(
                    new OrderItem(1L, 1, Money.of(10000))
            );
            
            assertThatThrownBy(() -> Order.create("", items))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("회원 ID는 필수입니다");
            
            assertThatThrownBy(() -> Order.create("   ", items))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("회원 ID는 필수입니다");
        }

        @DisplayName("주문 항목이 null이거나 비어있으면 예외가 발생한다")
        @Test
        void shouldThrowException_whenItemsAreNullOrEmpty() {
            assertThatThrownBy(() -> Order.create("member1", null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("주문 항목은 필수입니다");
            
            assertThatThrownBy(() -> Order.create("member1", List.of()))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("주문 항목은 필수입니다");
        }
    }

    @DisplayName("총액 계산")
    @Nested
    class CalculateTotalPrice {

        @DisplayName("총액이 모든 주문 항목의 합계로 정확히 계산된다")
        @Test
        void shouldCalculateTotalPriceCorrectly_whenMultipleItems() {
            List<OrderItem> items = List.of(
                    new OrderItem(1L, 2, Money.of(5000)),   // 10000
                    new OrderItem(2L, 3, Money.of(8000)),   // 24000
                    new OrderItem(3L, 1, Money.of(12000))   // 12000
            );
            
            Order order = Order.create("member1", items);
            
            assertThat(order.getTotalPrice()).isEqualTo(Money.of(46000));
        }

        @DisplayName("단일 항목의 경우 총액이 해당 항목의 총액과 동일하다")
        @Test
        void shouldEqualItemTotalPrice_whenSingleItem() {
            OrderItem item = new OrderItem(1L, 3, Money.of(10000));
            List<OrderItem> items = List.of(item);
            
            Order order = Order.create("member1", items);
            
            assertThat(order.getTotalPrice()).isEqualTo(item.getTotalPrice());
            assertThat(order.getTotalPrice()).isEqualTo(Money.of(30000));
        }
    }

    @DisplayName("주문 항목 관리")
    @Nested
    class ManageOrderItems {

        @DisplayName("주문 항목 목록이 불변으로 반환된다")
        @Test
        void shouldReturnImmutableItemList_whenGetItems() {
            List<OrderItem> items = List.of(
                    new OrderItem(1L, 1, Money.of(10000))
            );
            
            Order order = Order.create("member1", items);
            List<OrderItem> returnedItems = order.getItems();
            
            // 반환된 리스트 수정 시도 시 예외 발생
            assertThatThrownBy(() -> returnedItems.add(new OrderItem(2L, 1, Money.of(5000))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @DisplayName("주문 항목 개수를 정확히 반환한다")
        @Test
        void shouldReturnCorrectItemCount_whenMultipleItems() {
            List<OrderItem> items = List.of(
                    new OrderItem(1L, 2, Money.of(5000)),
                    new OrderItem(2L, 1, Money.of(10000)),
                    new OrderItem(3L, 3, Money.of(3000))
            );
            
            Order order = Order.create("member1", items);
            
            assertThat(order.getItems()).hasSize(3);
        }
    }
}