package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderItem 도메인 테스트")
class OrderItemTest {

    private Product createTestProduct(String name, BigDecimal price) {
        Brand brand = Brand.builder()
            .name("테스트 브랜드")
            .description("테스트용")
            .build();

        return Product.builder()
            .brand(brand)
            .name(name)
            .price(price)
            .stock(100)
            .build();
    }

    @DisplayName("주문 항목 생성 시")
    @Nested
    class CreateOrderItem {

        @DisplayName("정상적으로 주문 항목을 생성할 수 있다 (스냅샷 패턴)")
        @Test
        void createOrderItem_success() {
            // given
            Product product = createTestProduct("테스트 상품", BigDecimal.valueOf(10000));
            Integer quantity = 2;

            // when
            OrderItem orderItem = OrderItem.from(product, quantity);

            // then
            assertThat(orderItem.getProductId()).isEqualTo(product.getId());
            assertThat(orderItem.getProductName()).isEqualTo("테스트 상품");
            assertThat(orderItem.getBrandName()).isEqualTo("테스트 브랜드");
            assertThat(orderItem.getQuantity()).isEqualTo(quantity);
            assertThat(orderItem.getPrice()).isEqualByComparingTo(product.getPrice());
        }

        @DisplayName("상품 ID가 null이면 예외가 발생한다")
        @Test
        void createOrderItem_withNullProductId_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                OrderItem.builder()
                    .productId(null)
                    .productName("상품")
                    .quantity(1)
                    .price(BigDecimal.valueOf(10000))
                    .build()
            );
        }

        @DisplayName("상품명이 null이면 예외가 발생한다")
        @Test
        void createOrderItem_withNullProductName_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                OrderItem.builder()
                    .productId(1L)
                    .productName(null)
                    .quantity(1)
                    .price(BigDecimal.valueOf(10000))
                    .build()
            );
        }

        @DisplayName("수량이 0 이하이면 예외가 발생한다")
        @Test
        void createOrderItem_withZeroQuantity_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                OrderItem.builder()
                    .productId(1L)
                    .productName("상품")
                    .brandName("브랜드")
                    .quantity(0)
                    .price(BigDecimal.valueOf(10000))
                    .build()
            );
        }

        @DisplayName("가격이 음수이면 예외가 발생한다")
        @Test
        void createOrderItem_withNegativePrice_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                OrderItem.builder()
                    .productId(1L)
                    .productName("상품")
                    .brandName("브랜드")
                    .quantity(1)
                    .price(BigDecimal.valueOf(-1000))
                    .build()
            );
        }
    }

    @DisplayName("금액 계산 시")
    @Nested
    class CalculateAmount {

        @DisplayName("가격 * 수량으로 금액을 계산한다")
        @Test
        void calculateAmount_success() {
            // given
            Product product = createTestProduct("상품", BigDecimal.valueOf(10000));
            OrderItem orderItem = OrderItem.from(product, 3);

            // when
            BigDecimal amount = orderItem.calculateAmount();

            // then
            assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(30000));
        }

        @DisplayName("상품 가격이 변경되어도 주문 당시 가격으로 계산한다")
        @Test
        void calculateAmount_withChangedProductPrice_usesSnapshotPrice() {
            // given
            Product product = createTestProduct("상품", BigDecimal.valueOf(10000));
            OrderItem orderItem = OrderItem.from(product, 2);  // 주문 당시 가격 10000원 스냅샷

            // 상품 가격 변경
            product.updateInfo(null, BigDecimal.valueOf(20000), null, null);

            // when
            BigDecimal amount = orderItem.calculateAmount();

            // then
            // 주문 당시 가격 (10000 * 2 = 20000)으로 계산되어야 함
            // 상품 가격이 20000원으로 변경되었지만 OrderItem은 스냅샷 가격 사용
            assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(20000));
            assertThat(orderItem.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        }
    }
}
