package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

    @DisplayName("주문을 생성할 때,")
    @Nested
    class CreateOrder {

        @DisplayName("모든 필드가 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsOrder_whenAllFieldsAreValid() {
            // arrange
            Long userId = 1L;
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");

            // act
            Order order = Order.createOrder(userId, delivery);

            // assert
            assertAll(
                    () -> assertThat(order.getUserId()).isEqualTo(userId),
                    () -> assertThat(order.getDelivery()).isEqualTo(delivery),
                    () -> assertThat(order.getTotalPrice()).isEqualTo(0),
                    () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED),
                    () -> assertThat(order.getOrderItems()).isEmpty()
            );
        }

        @DisplayName("사용자 ID가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenUserIdIsNull() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Order.createOrder(null, delivery);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("사용자 ID는 필수입니다.");
        }

        @DisplayName("배송 정보가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenDeliveryIsNull() {
            // arrange
            Long userId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                Order.createOrder(userId, null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("배송 정보는 필수입니다.");
        }
    }

    @DisplayName("주문 상품을 추가할 때,")
    @Nested
    class AddOrderItem {

        @DisplayName("주문 상품이 올바르게 주어지면 정상적으로 추가된다.")
        @Test
        void addsOrderItem_whenOrderItemIsValid() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);
            OrderItem orderItem = OrderItem.createOrderItem(1L, "상품명", 10000, 2);

            // act
            order.addOrderItem(orderItem);

            // assert
            assertThat(order.getOrderItems()).hasSize(1);
            assertThat(order.getOrderItems().get(0)).isEqualTo(orderItem);
        }

        @DisplayName("주문 상품이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenOrderItemIsNull() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                order.addOrderItem(null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("주문 상품은 필수입니다.");
        }
    }

    @DisplayName("가격을 추가할 때,")
    @Nested
    class AddPrice {

        @DisplayName("가격이 올바르게 주어지면 정상적으로 추가된다.")
        @Test
        void addsPrice_whenPriceIsValid() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);
            int price = 10000;

            // act
            order.addPrice(price);

            // assert
            assertThat(order.getTotalPrice()).isEqualTo(10000);
        }

        @DisplayName("가격을 여러 번 추가하면 총액이 누적된다.")
        @Test
        void accumulatesPrice_whenPriceIsAddedMultipleTimes() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);

            // act
            order.addPrice(10000);
            order.addPrice(5000);
            order.addPrice(3000);

            // assert
            assertThat(order.getTotalPrice()).isEqualTo(18000);
        }

        @DisplayName("가격이 0 이하면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void throwsException_whenPriceIsZeroOrNegative(int invalidPrice) {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                order.addPrice(invalidPrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("가격은 0보다 커야 합니다.");
        }
    }
}
