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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderItemTest {

    @DisplayName("주문 상품을 생성할 때,")
    @Nested
    class CreateOrderItem {

        @DisplayName("모든 필드가 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsOrderItem_whenAllFieldsAreValid() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer price = 10000;
            Integer quantity = 2;

            // act
            OrderItem orderItem = OrderItem.createOrderItem(productId, productName, price, quantity);

            // assert
            assertAll(
                    () -> assertThat(orderItem.getProductId()).isEqualTo(productId),
                    () -> assertThat(orderItem.getProductName()).isEqualTo(productName),
                    () -> assertThat(orderItem.getPrice()).isEqualTo(price),
                    () -> assertThat(orderItem.getQuantity()).isEqualTo(quantity)
            );
        }

        @DisplayName("상품 ID가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenProductIdIsNull() {
            // arrange
            String productName = "상품명";
            Integer price = 10000;
            Integer quantity = 2;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.createOrderItem(null, productName, price, quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("상품 ID는 필수입니다.");
        }

        @DisplayName("상품명이 비어있으면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void throwsException_whenProductNameIsBlank(String invalidProductName) {
            // arrange
            Long productId = 1L;
            Integer price = 10000;
            Integer quantity = 2;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.createOrderItem(productId, invalidProductName, price, quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("상품명은 필수입니다.");
        }

        @DisplayName("가격이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenPriceIsNull() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = 2;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.createOrderItem(productId, productName, null, quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("가격은 필수입니다.");
        }

        @DisplayName("가격이 0 이하면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void throwsException_whenPriceIsZeroOrNegative(int invalidPrice) {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = 2;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.createOrderItem(productId, productName, invalidPrice, quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("가격은 0보다 커야 합니다.");
        }

        @DisplayName("수량이 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenQuantityIsNull() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer price = 10000;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.createOrderItem(productId, productName, price, null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("수량은 필수입니다.");
        }

        @DisplayName("수량이 0 이하면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        void throwsException_whenQuantityIsZeroOrNegative(int invalidQuantity) {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer price = 10000;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.createOrderItem(productId, productName, price, invalidQuantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("수량은 0보다 커야 합니다.");
        }
    }
}
