package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @DisplayName("주문 아이템을 생성할 때, ")
    @Nested
    class Create {

        @Test
        void 상품개수_상품가격_상품_PK_ID가_주어지면_정상적으로_생성된다() {
            // arrange
            Integer quantity = 1;
            Integer productPrice = 10000;
            Long productId = 1L;
            Long orderId = 1L;

            // act
            OrderItem orderItem = OrderItem.create(quantity, productPrice, productId, orderId);

            // assert
            assertAll(
                    () -> assertThat(orderItem.getId()).isNotNull(),
                    () -> assertThat(orderItem.getQuantity()).isEqualTo(1),
                    () -> assertThat(orderItem.getProductPrice()).isEqualTo(10000),
                    () -> assertThat(orderItem.getProductId()).isEqualTo(1L),
                    () -> assertThat(orderItem.getOrderId()).isEqualTo(1L)
            );
        }

        @Test
        void 상품개수가_없으면_오류가_발생된다() {
            // arrange
            Integer productPrice = 10000;
            Long productId = 1L;
            Long orderId = 1L;

            // act && assert
            assertThatThrownBy(() -> OrderItem.create(null, productPrice, productId, orderId))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("상품 개수는 비어있을 수 없습니다.");
                    });
        }

        @Test
        void 상품가격이_없으면_오류가_발생된다() {
            // arrange
            Integer quantity = 1;
            Long productId = 1L;
            Long orderId = 1L;

            // act && assert
            assertThatThrownBy(() -> OrderItem.create(quantity, null, productId, orderId))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("상품 가격은 비어있을 수 없습니다.");
                    });
        }

        @Test
        void 상품_PK_ID가_없으면_오류가_발생된다() {
            // arrange
            Integer quantity = 1;
            Integer productPrice = 10000;
            Long orderId = 1L;

            // act && assert
            assertThatThrownBy(() -> OrderItem.create(quantity, productPrice, null, orderId))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("상품은 비어있을 수 없습니다.");
                    });
        }

        @Test
        void 주문_PK_ID가_없으면_오류가_발생된다() {
            // arrange
            Integer quantity = 1;
            Integer productPrice = 10000;
            Long productId = 1L;

            // act && assert
            assertThatThrownBy(() -> OrderItem.create(quantity, productPrice, productId, null))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("주문은 비어있을 수 없습니다.");
                    });
        }
    }
}
