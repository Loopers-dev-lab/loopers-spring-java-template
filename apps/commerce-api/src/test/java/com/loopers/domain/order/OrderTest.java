package com.loopers.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order Entity 테스트")
class OrderTest {

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("성공 - 정상적인 주문 생성")
        void createOrder_Success() {
            // given
            String userId = "user123";
            List<OrderItem> orderItems = Arrays.asList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 2),
                    OrderItem.create(2L, "상품2", new BigDecimal("5000"), 1)
            );
            int usedPoints = 1000;

            // when
            Order order = Order.create(userId, orderItems, usedPoints);

            // then
            assertThat(order).isNotNull();
            assertThat(order.getUserId()).isEqualTo(userId);
            assertThat(order.getOrderItems()).hasSize(2);
            assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("25000")); // 10000*2 + 5000*1
            assertThat(order.getUsedPoints()).isEqualTo(1000);
            assertThat(order.getFinalAmount()).isEqualTo(new BigDecimal("24000")); // 25000 - 1000
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getOrderedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 포인트를 사용하지 않는 주문")
        void createOrder_WithoutPoints() {
            // given
            String userId = "user123";
            List<OrderItem> orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );

            // when
            Order order = Order.create(userId, orderItems, 0);

            // then
            assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("10000"));
            assertThat(order.getUsedPoints()).isZero();
            assertThat(order.getFinalAmount()).isEqualTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("실패 - 사용자 ID가 null")
        void createOrder_NullUserId() {
            // given
            List<OrderItem> orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );

            // when & then
            assertThatThrownBy(() -> Order.create(null, orderItems, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 사용자 ID가 빈 문자열")
        void createOrder_EmptyUserId() {
            // given
            List<OrderItem> orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );

            // when & then
            assertThatThrownBy(() -> Order.create("  ", orderItems, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 주문 항목이 null")
        void createOrder_NullOrderItems() {
            // when & then
            assertThatThrownBy(() -> Order.create("user123", null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 항목은 최소 1개 이상이어야 합니다");
        }

        @Test
        @DisplayName("실패 - 주문 항목이 비어있음")
        void createOrder_EmptyOrderItems() {
            // when & then
            assertThatThrownBy(() -> Order.create("user123", Collections.emptyList(), 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 항목은 최소 1개 이상이어야 합니다");
        }

        @Test
        @DisplayName("실패 - 사용 포인트가 음수")
        void createOrder_NegativePoints() {
            // given
            List<OrderItem> orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );

            // when & then
            assertThatThrownBy(() -> Order.create("user123", orderItems, -100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용 포인트는 0 이상이어야 합니다: -100");
        }

        @Test
        @DisplayName("실패 - 사용 포인트가 총 금액 초과")
        void createOrder_PointsExceedTotal() {
            // given
            List<OrderItem> orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );

            // when & then
            assertThatThrownBy(() -> Order.create("user123", orderItems, 15000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용 포인트(15000)가 총 주문 금액(10000)을 초과할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("주문 완료")
    class CompleteOrder {

        @Test
        @DisplayName("성공 - PENDING 상태의 주문 완료")
        void completeOrder_FromPending() {
            // given
            Order order = createSampleOrder();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

            // when
            order.complete();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.isCompleted()).isTrue();
            assertThat(order.getModifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 이미 완료된 주문 (멱등성)")
        void completeOrder_AlreadyCompleted() {
            // given
            Order order = createSampleOrder();
            order.complete();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

            // when
            order.complete(); // 다시 완료

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("실패 - 취소된 주문은 완료할 수 없음")
        void completeOrder_FromCancelled() {
            // given
            Order order = createSampleOrder();
            order.cancel();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            // when & then
            assertThatThrownBy(order::complete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("취소된 주문은 완료할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelOrder {

        @Test
        @DisplayName("성공 - PENDING 상태의 주문 취소")
        void cancelOrder_FromPending() {
            // given
            Order order = createSampleOrder();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.isCancelled()).isTrue();
            assertThat(order.getModifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 이미 취소된 주문 (멱등성)")
        void cancelOrder_AlreadyCancelled() {
            // given
            Order order = createSampleOrder();
            order.cancel();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            // when
            order.cancel(); // 다시 취소

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("실패 - 완료된 주문은 취소할 수 없음")
        void cancelOrder_FromCompleted() {
            // given
            Order order = createSampleOrder();
            order.complete();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

            // when & then
            assertThatThrownBy(order::cancel)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("완료된 주문은 취소할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("주문 상태 확인")
    class OrderStatusCheck {

        @Test
        @DisplayName("주문 소유자 확인")
        void isOwnedBy() {
            // given
            Order order = createSampleOrder();

            // when & then
            assertThat(order.isOwnedBy("user123")).isTrue();
            assertThat(order.isOwnedBy("other-user")).isFalse();
        }

        @Test
        @DisplayName("대기 상태 확인")
        void isPending() {
            // given
            Order order = createSampleOrder();

            // then
            assertThat(order.isPending()).isTrue();
            assertThat(order.isCompleted()).isFalse();
            assertThat(order.isCancelled()).isFalse();
        }

        @Test
        @DisplayName("완료 상태 확인")
        void isCompleted() {
            // given
            Order order = createSampleOrder();
            order.complete();

            // then
            assertThat(order.isCompleted()).isTrue();
            assertThat(order.isPending()).isFalse();
            assertThat(order.isCancelled()).isFalse();
        }

        @Test
        @DisplayName("취소 상태 확인")
        void isCancelled() {
            // given
            Order order = createSampleOrder();
            order.cancel();

            // then
            assertThat(order.isCancelled()).isTrue();
            assertThat(order.isPending()).isFalse();
            assertThat(order.isCompleted()).isFalse();
        }
    }

    // === Helper Methods ===

    private Order createSampleOrder() {
        List<OrderItem> orderItems = Collections.singletonList(
                OrderItem.create(1L, "상품1", new BigDecimal("10000"), 2)
        );
        return Order.create("user123", orderItems, 1000);
    }
}
