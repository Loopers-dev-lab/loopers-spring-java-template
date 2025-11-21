package com.loopers.domain.order;

import com.loopers.domain.common.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderItemTest {
    @DisplayName("주문 상품을 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("상품 ID가 음수 혹은 null일 경우, 등록 실패한다.")
        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {-1L, -100L})
        void fails_whenProductIdIsNullOrNegative(Long invalidProductId) {
            // act + assert
            assertThatThrownBy(() -> OrderItem.create(invalidProductId,10, Money.of(1000), Money.of(10000)))
                    .hasMessageContaining("상품 ID는 음수 혹은 null일 수 없습니다.");
        }

        @DisplayName("주문 수량이 1 미만인 경우, 등록 실패한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -10})
        void fails_whenQuantityIsZeroOrNegative(int invalidQuantity) {
            // act + assert
            assertThatThrownBy(() -> OrderItem.create(1L, invalidQuantity, Money.of(1000), Money.of(10000)))
                    .hasMessageContaining("주문 수량은 1 이상이어야 합니다.");
        }

        @DisplayName("총 주문 금액이 (수량 * 단가) 결과와 다를 경우, 등록 실패한다.")
        @Test
        void fails_whenTotalPriceIsInvalid() {
            // act + assert
            assertThatThrownBy(() -> OrderItem.create(1L, 2, Money.of(1000), Money.of(10000)))
                    .hasMessageContaining("(수량 * 단가)가 총 주문 금액과 맞지 않습니다.");
        }

        @DisplayName("상품ID, 주문 수량, 주문 금액이 정상적으로 제공된 경우, 등록 성공한다.")
        @Test
        void succeeds_whenProductIdQtyAndPriceAreValid() {
            // act
            OrderItem item = OrderItem.create(1L, 10, Money.of(1000), Money.of(10000));

            // assert
            assertAll(
                    () -> assertThat(item).isNotNull(),
                    () -> assertThat(item.getProductId()).isEqualTo(1L),
                    () -> assertThat(item.getQuantity()).isEqualTo(10),
                    () -> assertThat(item.getUnitPrice().getAmount()).isEqualByComparingTo("1000"),
                    () -> assertThat(item.getTotalPrice().getAmount()).isEqualByComparingTo("10000")
            );
        }
    }
}
