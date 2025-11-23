package com.loopers.domain.order;

import com.loopers.domain.common.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OrderTest {
    @DisplayName("주문 등록할 때, ")
    @Nested
    class Create {

        @DisplayName("주문 유저 ID가 음수 혹은 null일 경우, 등록 실패한다.")
        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {-1L, -100L})
        void fails_whenUserIdIsNullOrNegative(Long invalidUserId) {
            // arrange + act + assert
            assertThatThrownBy(() -> Order.create(invalidUserId))
                    .hasMessageContaining("유저 ID는 음수 혹은 null일 수 없습니다.");
        }

        @DisplayName("유효한 주문 유저 ID로 상품 추가 시, 등록 성공한다.")
        @Test
        void succeeds_whenUserIdIsValidAndProductIsValid() {
            // arrange
            Order order = Order.create(1L);

            // act
            order.addItem(1L, 10, Money.of(1000), Money.of(10000));
            order.addItem(2L, 20, Money.of(2000), Money.of(40000));

            // assert
            assertThat(order.getItems().size()).isEqualTo(2);
        }

    }
}
