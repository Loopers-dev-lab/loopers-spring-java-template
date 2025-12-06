package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class OrderTest {

    static final String USER_ID = "user123";
    static final Long PRODUCT_ID_1 = 1L;

    @Nested
    @DisplayName("Order 상태 전환 - startPayment")
    class StartPaymentTest {

        @Test
        @DisplayName("PENDING 상태에서 startPayment 호출 시 PAYING 상태로 전환된다")
        void test1() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);

            order.startPayment();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYING);
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 startPayment 호출 시 예외 발생")
        void test2() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);
            order.startPayment();

            assertThatThrownBy(order::startPayment)
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PENDING 상태에서만 결제를 시작할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("Order 상태 전환 - confirm")
    class ConfirmTest {

        @Test
        @DisplayName("PAYING 상태에서 confirm 호출 시 CONFIRMED 상태로 전환된다")
        void test1() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);
            order.startPayment();

            order.confirm();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("PAYING이 아닌 상태에서 confirm 호출 시 예외 발생")
        void test2() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);

            assertThatThrownBy(order::confirm)
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PAYING 상태에서만 확정할 수 있습니다");
        }

        @Test
        @DisplayName("PENDING 상태에서 confirm 호출 시 예외 발생")
        void test3() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);

            assertThatThrownBy(order::confirm)
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PAYING 상태에서만 확정할 수 있습니다");
        }

    }

    @Nested
    @DisplayName("Order 전체 상태 머신")
    class FullStateMachineTest {

        @Test
        @DisplayName("정상 상태 전환: PENDING -> PAYING -> CONFIRMED")
        void test1() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

            order.startPayment();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYING);

            order.confirm();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Order 상태 전이 멱등성")
    class OrderIdempotencyTest {

        @Test
        @DisplayName("CONFIRMED 상태에서 confirm 재호출 시 성공한다")
        void confirmTest1() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);
            order.startPayment();

            order.confirm();
            order.confirm();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("FAILED 상태에서 fail 재호출 시 성공한다")
        void failTest1() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);
            order.startPayment();

            order.fail();
            order.fail();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        }

        @Test
        @DisplayName("PENDING에서 confirm 호출 시 예외")
        void confirmTest2() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);

            assertThatThrownBy(() -> order.confirm())
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PAYING 상태에서만");
        }

        @Test
        @DisplayName("CONFIRMED에서 fail 호출 시 예외")
        void failTest2() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            Order order = Order.create(USER_ID, List.of(item), 10_000);
            order.startPayment();
            order.confirm();

            assertThatThrownBy(() -> order.fail())
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PAYING 상태에서만");
        }
    }
}
