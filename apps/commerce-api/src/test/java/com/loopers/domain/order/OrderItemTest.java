package com.loopers.domain.order;

import com.loopers.domain.common.vo.Money;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrderItemTest {

    @DisplayName("주문 항목 생성")
    @Nested
    class CreateOrderItem {

        @DisplayName("정상적인 주문 항목이 성공적으로 생성된다")
        @Test
        void shouldCreateOrderItem_whenValidInput() {
            OrderItem orderItem = new OrderItem(1L, 2, Money.of(10000));
            
            assertThat(orderItem.getProductId()).isEqualTo(1L);
            assertThat(orderItem.getQuantity()).isEqualTo(2);
            assertThat(orderItem.getUnitPrice()).isEqualTo(Money.of(10000));
            assertThat(orderItem.getTotalPrice()).isEqualTo(Money.of(20000));
        }

        @DisplayName("상품 ID가 null이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenProductIdIsNull() {
            assertThatThrownBy(() -> new OrderItem(null, 2, Money.of(10000)))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("상품 ID는 필수입니다");
        }

        @DisplayName("수량이 0 이하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenQuantityIsZeroOrNegative() {
            assertThatThrownBy(() -> new OrderItem(1L, 0, Money.of(10000)))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("주문 수량은 1 이상이어야 합니다");
            
            assertThatThrownBy(() -> new OrderItem(1L, -1, Money.of(10000)))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("주문 수량은 1 이상이어야 합니다");
        }

        @DisplayName("단가가 null이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenUnitPriceIsNull() {
            assertThatThrownBy(() -> new OrderItem(1L, 2, null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("단가는 필수입니다");
        }
    }

    @DisplayName("총액 계산")
    @Nested
    class CalculateTotalPrice {

        @DisplayName("총액이 단가 × 수량으로 정확히 계산된다")
        @Test
        void shouldCalculateTotalPriceCorrectly_whenValidInput() {
            OrderItem orderItem1 = new OrderItem(1L, 3, Money.of(5000));
            OrderItem orderItem2 = new OrderItem(2L, 1, Money.of(15000));
            OrderItem orderItem3 = new OrderItem(3L, 10, Money.of(1000));
            
            assertThat(orderItem1.getTotalPrice()).isEqualTo(Money.of(15000));
            assertThat(orderItem2.getTotalPrice()).isEqualTo(Money.of(15000));
            assertThat(orderItem3.getTotalPrice()).isEqualTo(Money.of(10000));
        }

        @DisplayName("수량이 1일 때 총액은 단가와 동일하다")
        @Test
        void shouldEqualUnitPrice_whenQuantityIsOne() {
            OrderItem orderItem = new OrderItem(1L, 1, Money.of(10000));
            
            assertThat(orderItem.getTotalPrice()).isEqualTo(orderItem.getUnitPrice());
        }
    }
}
