package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessages;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Order 도메인 모델 테스트")
public class OrderModelTest {

    private OrderItem item(long productId, int quantity, String unitPrice) {
        return OrderItem.builder()
                .order(null)
                .productId(productId)
                .quantity(quantity)
                .price(new BigDecimal(unitPrice))
                .build();
    }

    @Nested
    @DisplayName("정상 흐름")
    class SuccessFlow {
        @Test
        @DisplayName("유효한 사용자와 아이템으로 주문이 생성된다")
        void creates_order_when_valid_input() {
            // given
            String userId = "user-1";
            List<OrderItem> items = List.of(
                    item(1L, 2, "1000"),
                    item(2L, 1, "5000")
            );

            // when
            Order order = Order.createOrder(userId, items);

            // then
            assertThat(order).isNotNull();
            assertThat(order.getUserId()).isEqualTo(userId);
            assertThat(order.getOrderItems()).hasSize(2);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("예외 흐름")
    class ExceptionFlow {
        @Test
        @DisplayName("userId가 비어 있으면 BAD_REQUEST가 발생한다")
        void throws_when_userId_blank() {
            // given
            String userId = "   ";
            List<OrderItem> items = List.of(item(1L, 1, "1000"));

            // when
            CoreException ex = assertThrows(CoreException.class, () -> Order.createOrder(userId, items));

            // then
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo(ErrorMessages.INVALID_NAME_FORMAT);
        }

        @Test
        @DisplayName("아이템 리스트가 비어 있으면 BAD_REQUEST가 발생한다")
        void throws_when_items_empty() {
            // given
            String userId = "user-1";
            List<OrderItem> items = List.of();

            // when
            CoreException ex = assertThrows(CoreException.class, () -> Order.createOrder(userId, items));

            // then
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo(ErrorMessages.INVALID_ORDER_ITEMS_LIST);
        }
    }
}
