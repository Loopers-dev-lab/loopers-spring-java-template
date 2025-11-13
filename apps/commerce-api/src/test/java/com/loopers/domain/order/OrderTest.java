package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrderTest {

    @DisplayName("정상 주문 / 예외 주문 흐름을 모두 검증한다.")
    @Nested
    class OrderFlow {
        @DisplayName("정상 주문 흐름: 주문이 정상적으로 생성되고 총액이 올바르게 계산된다.")
        @Test
        void normalOrderFlow() {
            // arrange
            Long userId = OrderTestFixture.ValidOrder.USER_ID;
            List<OrderItem> items = OrderTestFixture.ValidOrderItem.createMultipleItems();
            // 상품 1: 10000 * 1 = 10000
            // 상품 2: 5000 * 2 = 10000
            // 총액: 20000

            // act
            Order order = Order.of(userId, items);

            // assert
            assertThat(order.getUserId()).isEqualTo(userId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getTotalAmount()).isEqualTo(20000);
            assertThat(order.getItems()).hasSize(2);
            assertThat(order.getItems()).containsExactlyElementsOf(items);
        }

        @DisplayName("예외 주문 흐름: 주문 아이템이 null이면 예외가 발생한다.")
        @Test
        void exceptionOrderFlow_nullItems() {
            // arrange
            Long userId = OrderTestFixture.ValidOrder.USER_ID;

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                Order.of(userId, null);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("주문 아이템은 필수이며 최소 1개 이상이어야 합니다");
        }

        @DisplayName("예외 주문 흐름: 주문 아이템이 비어있으면 예외가 발생한다.")
        @Test
        void exceptionOrderFlow_emptyItems() {
            // arrange
            Long userId = OrderTestFixture.ValidOrder.USER_ID;
            List<OrderItem> emptyItems = List.of();

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                Order.of(userId, emptyItems);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("주문 아이템은 필수이며 최소 1개 이상이어야 합니다");
        }

        @DisplayName("예외 주문 흐름: 사용자 ID가 null이면 예외가 발생한다.")
        @Test
        void exceptionOrderFlow_nullUserId() {
            // arrange
            List<OrderItem> items = OrderTestFixture.ValidOrderItem.createMultipleItems();

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                Order.of(null, items);
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("사용자 ID는 필수입니다");
        }
    }

}


