package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Order 도메인 테스트")
class OrderTest {

    private Product createTestProduct(String name, BigDecimal price, Integer stock) {
        Brand brand = Brand.builder()
            .name("테스트 브랜드")
            .description("테스트용")
            .build();

        return Product.builder()
            .brand(brand)
            .name(name)
            .price(price)
            .stock(stock)
            .build();
    }

    @DisplayName("주문 생성 시")
    @Nested
    class CreateOrder {

        @DisplayName("정상적으로 주문을 생성할 수 있다")
        @Test
        void createOrder_success() {
            // given
            String userId = "user123";

            // when
            Order order = Order.builder()
                .userId(userId)
                .build();

            // then
            assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(userId),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO)
            );
        }

        @DisplayName("userId가 null이면 예외가 발생한다")
        @Test
        void createOrder_withNullUserId_throwsException() {
            // when & then
            assertThrows(CoreException.class, () ->
                Order.builder()
                    .userId(null)
                    .build()
            );
        }
    }

    @DisplayName("주문 항목 추가 시")
    @Nested
    class AddOrderItem {

        @DisplayName("주문 항목을 정상적으로 추가할 수 있다")
        @Test
        void addOrderItem_success() {
            // given
            Order order = Order.builder()
                .userId("user123")
                .build();

            Product product = createTestProduct("상품", BigDecimal.valueOf(10000), 10);

            OrderItem orderItem = OrderItem.builder()
                .product(product)
                .quantity(2)
                .price(product.getPrice())
                .build();

            // when
            order.addOrderItem(orderItem);

            // then
            assertThat(order.getOrderItems()).hasSize(1);
            assertThat(order.getOrderItems().get(0)).isEqualTo(orderItem);
        }
    }

    @DisplayName("총 금액 계산 시")
    @Nested
    class CalculateTotalAmount {

        @DisplayName("주문 항목들의 금액을 합산한다")
        @Test
        void calculateTotalAmount_success() {
            // given
            Order order = Order.builder()
                .userId("user123")
                .build();

            Product product1 = createTestProduct("상품1", BigDecimal.valueOf(10000), 10);
            Product product2 = createTestProduct("상품2", BigDecimal.valueOf(20000), 10);

            OrderItem orderItem1 = OrderItem.builder()
                .product(product1)
                .quantity(2)
                .price(product1.getPrice())
                .build();

            OrderItem orderItem2 = OrderItem.builder()
                .product(product2)
                .quantity(1)
                .price(product2.getPrice())
                .build();

            order.addOrderItem(orderItem1);
            order.addOrderItem(orderItem2);

            // when
            order.calculateTotalAmount();

            // then
            // 10000 * 2 + 20000 * 1 = 40000
            assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        }
    }

    @DisplayName("주문 취소 시")
    @Nested
    class CancelOrder {

        @DisplayName("대기 상태의 주문은 취소할 수 있다")
        @Test
        void cancel_pendingOrder_success() {
            // given
            Order order = Order.builder()
                .userId("user123")
                .build();

            // when
            order.cancel();

            // then
            assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED),
                () -> assertThat(order.getCanceledAt()).isNotNull()
            );
        }

        @DisplayName("완료된 주문은 취소할 수 없다")
        @Test
        void cancel_completedOrder_throwsException() {
            // given
            Order order = Order.builder()
                .userId("user123")
                .build();
            order.complete();

            // when & then
            CoreException exception = assertThrows(CoreException.class, () ->
                order.cancel()
            );

            assertThat(exception.getMessage()).contains("배송 시작 후에는 취소할 수 없습니다");
        }
    }

    @DisplayName("취소 가능 여부 확인 시")
    @Nested
    class CanCancel {

        @DisplayName("대기 상태의 주문은 취소 가능하다")
        @Test
        void canCancel_pendingOrder_returnsTrue() {
            // given
            Order order = Order.builder()
                .userId("user123")
                .build();

            // when & then
            assertThat(order.canCancel()).isTrue();
        }

        @DisplayName("완료된 주문은 취소 불가능하다")
        @Test
        void canCancel_completedOrder_returnsFalse() {
            // given
            Order order = Order.builder()
                .userId("user123")
                .build();
            order.complete();

            // when & then
            assertThat(order.canCancel()).isFalse();
        }

        @DisplayName("이미 취소된 주문은 취소 불가능하다")
        @Test
        void canCancel_canceledOrder_returnsFalse() {
            // given
            Order order = Order.builder()
                .userId("user123")
                .build();
            order.cancel();

            // when & then
            assertThat(order.canCancel()).isFalse();
        }
    }
}
