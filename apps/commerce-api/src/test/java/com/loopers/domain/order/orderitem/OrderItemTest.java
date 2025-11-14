package com.loopers.domain.order.orderitem;

import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.quantity.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderItem 도메인 테스트")
class OrderItemTest {

  @DisplayName("OrderItem을 생성할 때")
  @Nested
  class Create {

    @DisplayName("올바른 정보로 생성하면 성공한다")
    @Test
    void shouldCreate_whenValid() {
      // given
      Long productId = 100L;
      String productName = "테스트 상품";
      Quantity quantity = Quantity.of(3);
      OrderPrice orderPrice = OrderPrice.of(10000L);

      // when
      OrderItem orderItem = OrderItem.of(productId, productName, quantity, orderPrice);

      // then
      assertThat(orderItem).extracting("productId", "productName")
          .containsExactly(productId, productName);
      assertThat(orderItem).extracting("quantity", "orderPrice")
          .containsExactly(quantity, orderPrice);
    }
  }

  @DisplayName("productId 검증")
  @Nested
  class ValidateProductId {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      // given
      String productName = "테스트 상품";
      Quantity quantity = Quantity.of(1);
      OrderPrice orderPrice = OrderPrice.of(10000L);

      // when & then
      assertThatThrownBy(() -> OrderItem.of(null, productName, quantity, orderPrice))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_ITEM_PRODUCT_EMPTY);
    }
  }

  @DisplayName("productName 검증")
  @Nested
  class ValidateProductName {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      // given
      Long productId = 100L;
      Quantity quantity = Quantity.of(1);
      OrderPrice orderPrice = OrderPrice.of(10000L);

      // when & then
      assertThatThrownBy(() -> OrderItem.of(productId, null, quantity, orderPrice))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품명은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_ITEM_PRODUCT_NAME_EMPTY);
    }

    @DisplayName("빈 문자열이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenBlank() {
      // given
      Long productId = 100L;
      Quantity quantity = Quantity.of(1);
      OrderPrice orderPrice = OrderPrice.of(10000L);

      // when & then
      assertThatThrownBy(() -> OrderItem.of(productId, "  ", quantity, orderPrice))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품명은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_ITEM_PRODUCT_NAME_EMPTY);
    }

    @DisplayName("100자를 초과하면 예외가 발생한다")
    @Test
    void shouldThrowException_whenTooLong() {
      // given
      Long productId = 100L;
      String productName = "a".repeat(101);
      Quantity quantity = Quantity.of(1);
      OrderPrice orderPrice = OrderPrice.of(10000L);

      // when & then
      assertThatThrownBy(() -> OrderItem.of(productId, productName, quantity, orderPrice))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품명은 100자 이내여야 합니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_ITEM_PRODUCT_NAME_LENGTH);
    }
  }

  @DisplayName("quantity 검증")
  @Nested
  class ValidateQuantity {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      // given
      Long productId = 100L;
      String productName = "테스트 상품";
      OrderPrice orderPrice = OrderPrice.of(10000L);

      // when & then
      assertThatThrownBy(() -> OrderItem.of(productId, productName, null, orderPrice))
          .isInstanceOf(CoreException.class)
          .hasMessage("수량은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_ITEM_QUANTITY_EMPTY);
    }
  }

  @DisplayName("orderPrice 검증")
  @Nested
  class ValidateOrderPrice {

    @DisplayName("null이면 예외가 발생한다")
    @Test
    void shouldThrowException_whenNull() {
      // given
      Long productId = 100L;
      String productName = "테스트 상품";
      Quantity quantity = Quantity.of(1);

      // when & then
      assertThatThrownBy(() -> OrderItem.of(productId, productName, quantity, null))
          .isInstanceOf(CoreException.class)
          .hasMessage("주문 가격은 비어있을 수 없습니다.")
          .extracting("errorType").isEqualTo(ErrorType.INVALID_ORDER_PRICE_VALUE);
    }
  }

  @DisplayName("비즈니스 로직")
  @Nested
  class BusinessLogic {

    @DisplayName("주문 가격 값을 조회한다")
    @Test
    void shouldGetOrderPriceValue() {
      // given
      Long productId = 100L;
      String productName = "테스트 상품";
      Quantity quantity = Quantity.of(1);
      OrderPrice orderPrice = OrderPrice.of(20000L);
      OrderItem orderItem = OrderItem.of(productId, productName, quantity, orderPrice);

      // when
      Long priceValue = orderItem.getOrderPriceValue();

      // then
      assertThat(priceValue).isEqualTo(20000L);
    }
  }
}
