package com.loopers.domain.order;

import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("주문 항목(OrderItem) Value Object 테스트")
public class OrderItemTest {

    @DisplayName("주문 항목을 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("정상적인 값으로 주문 항목을 생성할 수 있다. (Happy Path)")
        @Test
        void should_createOrderItem_when_validValues() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = 2;
            Price price = new Price(10000);

            // act
            OrderItem orderItem = OrderItem.create(productId, productName, quantity, price);

            // assert
            assertThat(orderItem.getProductId()).isEqualTo(1L);
            assertThat(orderItem.getProductName()).isEqualTo("상품명");
            assertThat(orderItem.getQuantity()).isEqualTo(2);
            assertThat(orderItem.getPricePerItem().amount()).isEqualTo(10000);
        }

        @DisplayName("수량이 0이면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_quantityIsZero() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = 0;
            Price price = new Price(10000);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.create(productId, productName, quantity, price);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("주문 수량은 1 이상의 자연수여야 합니다.");
        }

        @DisplayName("수량이 음수이면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_quantityIsNegative() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = -1;
            Price price = new Price(10000);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.create(productId, productName, quantity, price);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("주문 수량은 1 이상의 자연수여야 합니다.");
        }

        @DisplayName("가격이 0인 주문 항목을 생성할 수 있다. (Edge Case)")
        @Test
        void should_createOrderItem_when_priceIsZero() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = 1;
            Price price = new Price(0);

            // act
            OrderItem orderItem = OrderItem.create(productId, productName, quantity, price);

            // assert
            assertThat(orderItem.getPricePerItem().amount()).isEqualTo(0);
        }

        @DisplayName("productName이 null이면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productNameIsNull() {
            // arrange
            Long productId = 1L;
            String productName = null;
            Integer quantity = 1;
            Price price = new Price(10000);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.create(productId, productName, quantity, price);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("상품명은 필수이며 공백일 수 없습니다.");
        }

        @DisplayName("productName이 빈 문자열이면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productNameIsEmpty() {
            // arrange
            Long productId = 1L;
            String productName = "";
            Integer quantity = 1;
            Price price = new Price(10000);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.create(productId, productName, quantity, price);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("상품명은 필수이며 공백일 수 없습니다.");
        }

        @DisplayName("productName이 공백만 있으면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productNameIsBlank() {
            // arrange
            Long productId = 1L;
            String productName = "   ";
            Integer quantity = 1;
            Price price = new Price(10000);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.create(productId, productName, quantity, price);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("상품명은 필수이며 공백일 수 없습니다.");
        }

        @DisplayName("quantity가 null이면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_quantityIsNull() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = null;
            Price price = new Price(10000);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.create(productId, productName, quantity, price);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("주문 수량은 1 이상의 자연수여야 합니다.");
        }

        @DisplayName("price가 null이면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_priceIsNull() {
            // arrange
            Long productId = 1L;
            String productName = "상품명";
            Integer quantity = 1;
            Price price = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItem.create(productId, productName, quantity, price);
            });

            // assert
            assertThat(exception.getMessage()).isEqualTo("가격은 필수입니다.");
        }
    }

    @DisplayName("주문 항목의 총 가격을 계산할 때, ")
    @Nested
    class GetTotalPrice {
        @DisplayName("정상적인 수량과 가격으로 총 가격을 계산할 수 있다. (Happy Path)")
        @Test
        void should_calculateTotalPrice_when_validQuantityAndPrice() {
            // arrange
            OrderItem orderItem = OrderItem.create(1L, "상품명", 3, new Price(10000));

            // act
            Integer totalPrice = orderItem.getTotalPrice();

            // assert
            assertThat(totalPrice).isEqualTo(30000);
        }

        @DisplayName("수량이 1이면 가격과 동일하다. (Edge Case)")
        @Test
        void should_returnPrice_when_quantityIsOne() {
            // arrange
            OrderItem orderItem = OrderItem.create(1L, "상품명", 1, new Price(10000));

            // act
            Integer totalPrice = orderItem.getTotalPrice();

            // assert
            assertThat(totalPrice).isEqualTo(10000);
        }

        @DisplayName("가격이 0이면 총 가격이 0이다. (Edge Case)")
        @Test
        void should_returnZero_when_priceIsZero() {
            // arrange
            OrderItem orderItem = OrderItem.create(1L, "상품명", 3, new Price(0));

            // act
            Integer totalPrice = orderItem.getTotalPrice();

            // assert
            assertThat(totalPrice).isEqualTo(0);
        }
    }
}
