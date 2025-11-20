package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderTest {

    @DisplayName("주문을 생성할 때, ")
    @Nested
    class Create {

        @Test
        void 사용자_PK_ID가_주어지면_정상적으로_생성된다() {
            // arrange
            Long userId = 1L;

            // act
            Order order = Order.create(userId);

            // assert
            assertThat(order.getId()).isNotNull();
            assertThat(order.getUserId()).isEqualTo(1L);
        }

        @Test
        void 사용자_PK_ID가_없으면_오류가_발생된다() {
            // act && assert
            assertThatThrownBy(() -> Order.create(null))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("유저는 비어있을 수 없습니다.");
                    });
        }
    }
}
