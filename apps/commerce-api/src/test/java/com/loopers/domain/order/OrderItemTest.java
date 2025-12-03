package com.loopers.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderItem 테스트")
class OrderItemTest {

    @Nested
    @DisplayName("주문 항목 생성")
    class CreateOrderItem {

        @Test
        @DisplayName("성공 - 정상적인 주문 항목 생성")
        void createOrderItem_Success() {
            // given
            Long productId = 1L;
            String productName = "테스트 상품";
            BigDecimal price = new BigDecimal("10000");
            int quantity = 3;

            // when
            OrderItem orderItem = OrderItem.create(productId, productName, price, quantity);

            // then
            assertThat(orderItem).isNotNull();
            assertThat(orderItem.getId()).isNull(); // 신규 생성이므로 ID 없음
            assertThat(orderItem.getProductId()).isEqualTo(productId);
            assertThat(orderItem.getProductName()).isEqualTo(productName);
            assertThat(orderItem.getPrice()).isEqualTo(price);
            assertThat(orderItem.getQuantity()).isEqualTo(quantity);
            assertThat(orderItem.getSubtotal()).isEqualTo(new BigDecimal("30000")); // 10000 * 3
            assertThat(orderItem.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 기존 주문 항목 재구성")
        void reconstructOrderItem_Success() {
            // given
            Long id = 1L;
            Long productId = 100L;
            String productName = "테스트 상품";
            BigDecimal price = new BigDecimal("5000");
            int quantity = 2;

            // when
            OrderItem orderItem = OrderItem.reconstruct(id, productId, productName, price, quantity, null);

            // then
            assertThat(orderItem.getId()).isEqualTo(id);
            assertThat(orderItem.getProductId()).isEqualTo(productId);
            assertThat(orderItem.getSubtotal()).isEqualTo(new BigDecimal("10000")); // 5000 * 2
        }

        @Test
        @DisplayName("실패 - 상품 ID가 null")
        void createOrderItem_NullProductId() {
            // when & then
            assertThatThrownBy(() -> OrderItem.create(null, "상품명", new BigDecimal("1000"), 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 상품 ID가 빈 문자열")
        void createOrderItem_EmptyProductId() {
            // 삭제됨 - productId가 Long 타입으로 변경되어 빈 문자열 테스트 불필요
        }

        @Test
        @DisplayName("실패 - 상품명이 null")
        void createOrderItem_NullProductName() {
            // when & then
            assertThatThrownBy(() -> OrderItem.create(1L, null, new BigDecimal("1000"), 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품명은 필수입니다");
        }

        @Test
        @DisplayName("실패 - 상품명이 빈 문자열")
        void createOrderItem_EmptyProductName() {
            // when & then
            assertThatThrownBy(() -> OrderItem.create(1L, "  ", new BigDecimal("1000"), 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품명은 필수입니다");
        }

        @Test
        @DisplayName("실패 - 가격이 null")
        void createOrderItem_NullPrice() {
            // when & then
            assertThatThrownBy(() -> OrderItem.create(1L, "상품명", null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가격은 필수입니다");
        }

        @Test
        @DisplayName("실패 - 가격이 음수")
        void createOrderItem_NegativePrice() {
            // when & then
            assertThatThrownBy(() -> OrderItem.create(1L, "상품명", new BigDecimal("-1000"), 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가격은 0 이상이어야 합니다: -1000");
        }

        @Test
        @DisplayName("성공 - 가격이 0")
        void createOrderItem_ZeroPrice() {
            // when
            OrderItem orderItem = OrderItem.create(1L, "무료 상품", BigDecimal.ZERO, 1);

            // then
            assertThat(orderItem.getPrice()).isEqualTo(BigDecimal.ZERO);
            assertThat(orderItem.getSubtotal()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("실패 - 수량이 0")
        void createOrderItem_ZeroQuantity() {
            // when & then
            assertThatThrownBy(() -> OrderItem.create(1L, "상품명", new BigDecimal("1000"), 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수량은 1 이상이어야 합니다: 0");
        }

        @Test
        @DisplayName("실패 - 수량이 음수")
        void createOrderItem_NegativeQuantity() {
            // when & then
            assertThatThrownBy(() -> OrderItem.create(1L, "상품명", new BigDecimal("1000"), -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수량은 1 이상이어야 합니다: -1");
        }
    }

    @Nested
    @DisplayName("소계 계산")
    class CalculateSubtotal {

        @Test
        @DisplayName("정수 가격과 수량")
        void calculateSubtotal_Integer() {
            // given & when
            OrderItem orderItem = OrderItem.create(1L, "상품", new BigDecimal("1000"), 5);

            // then
            assertThat(orderItem.getSubtotal()).isEqualTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("소수점 가격")
        void calculateSubtotal_Decimal() {
            // given & when
            OrderItem orderItem = OrderItem.create(1L, "상품", new BigDecimal("999.99"), 3);

            // then
            assertThat(orderItem.getSubtotal()).isEqualTo(new BigDecimal("2999.97"));
        }

        @Test
        @DisplayName("큰 수량")
        void calculateSubtotal_LargeQuantity() {
            // given & when
            OrderItem orderItem = OrderItem.create(1L, "상품", new BigDecimal("100"), 1000);

            // then
            assertThat(orderItem.getSubtotal()).isEqualTo(new BigDecimal("100000"));
        }
    }

    @Nested
    @DisplayName("비즈니스 로직")
    class BusinessLogic {

        @Test
        @DisplayName("특정 상품인지 확인 - 일치")
        void isProduct_Match() {
            // given
            OrderItem orderItem = OrderItem.create(1L, "상품", new BigDecimal("1000"), 1);

            // when & then
            assertThat(orderItem.isProduct(1L)).isTrue();
        }

        @Test
        @DisplayName("특정 상품인지 확인 - 불일치")
        void isProduct_NotMatch() {
            // given
            OrderItem orderItem = OrderItem.create(1L, "상품", new BigDecimal("1000"), 1);

            // when & then
            assertThat(orderItem.isProduct(2L)).isFalse();
        }
    }
}
