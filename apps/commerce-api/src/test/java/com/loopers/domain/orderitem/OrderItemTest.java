package com.loopers.domain.orderitem;

import com.loopers.domain.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderItemTest {

    @DisplayName("OrderProduct를 정상적으로 생성할 수 있다.")
    @Test
    void createOrderProduct_withValidInputs_success() {
        // given
        Order mockOrder = mock(Order.class);
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(10000));
        int quantity = 2;

        // when
        OrderItem orderItem = new OrderItem(mockOrder, mockProduct, quantity);

        // then
        assertThat(orderItem.getOrder()).isEqualTo(mockOrder);
        assertThat(orderItem.getProduct()).isEqualTo(mockProduct);
        assertThat(orderItem.getQuantity()).isEqualTo(2);
    }

    @DisplayName("OrderItem 생성 시 상품의 가격이 price에 저장된다.")
    @Test
    void createOrderProduct_setPriceFromProduct() {
        // given
        Order mockOrder = mock(Order.class);
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(15000));
        int quantity = 1;

        // when
        OrderItem orderItem = new OrderItem(mockOrder, mockProduct, quantity);

        // then
        assertThat(orderItem.getPrice()).isEqualTo(Money.of(15000));
    }

    @DisplayName("OrderItem 생성 시 총 금액을 정확히 계산한다.")
    @Test
    void createOrderProduct_calculateTotalPrice_correctly() {
        // given
        Order mockOrder = mock(Order.class);
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(10000));
        int quantity = 2;

        // when
        OrderItem orderItem = new OrderItem(mockOrder, mockProduct, quantity);

        // then
        // 10000 * 2 = 20000
        assertThat(orderItem.getTotalPrice()).isEqualTo(Money.of(20000));
    }

    @DisplayName("OrderItem 생성 시 여러 개 수량의 총 금액을 정확히 계산한다.")
    @Test
    void createOrderProduct_withMultipleQuantity_calculateCorrectly() {
        // given
        Order mockOrder = mock(Order.class);
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(7500));
        int quantity = 5;

        // when
        OrderItem orderItem = new OrderItem(mockOrder, mockProduct, quantity);

        // then
        // 7500 * 5 = 37500
        assertThat(orderItem.getTotalPrice()).isEqualTo(Money.of(37500));
    }

    @DisplayName("Order가 null이면 예외가 발생한다.")
    @Test
    void createOrderProduct_withNullOrder_throwException() {
        // given
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(10000));
        int quantity = 1;

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            new OrderItem(null, mockProduct, quantity)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("주문은 필수입니다");
    }

    @DisplayName("Product가 null이면 예외가 발생한다.")
    @Test
    void createOrderProduct_withNullProduct_throwException() {
        // given
        Order mockOrder = mock(Order.class);
        int quantity = 1;

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            new OrderItem(mockOrder, null, quantity)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("상품은 필수입니다");
    }

    @DisplayName("수량이 0이면 예외가 발생한다.")
    @Test
    void createOrderProduct_withZeroQuantity_throwException() {
        // given
        Order mockOrder = mock(Order.class);
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(10000));
        int quantity = 0;

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            new OrderItem(mockOrder, mockProduct, quantity)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("수량은 1개 이상이어야 합니다");
    }

    @DisplayName("수량이 음수이면 예외가 발생한다.")
    @Test
    void createOrderProduct_withNegativeQuantity_throwException() {
        // given
        Order mockOrder = mock(Order.class);
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(10000));
        int quantity = -1;

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            new OrderItem(mockOrder, mockProduct, quantity)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("수량은 1개 이상이어야 합니다");
    }

    @DisplayName("수량이 1일 때 정상적으로 생성된다.")
    @Test
    void createOrderProduct_withQuantityOne_success() {
        // given
        Order mockOrder = mock(Order.class);
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(Money.of(12000));
        int quantity = 1;

        // when
        OrderItem orderItem = new OrderItem(mockOrder, mockProduct, quantity);

        // then
        assertThat(orderItem.getQuantity()).isEqualTo(1);
        assertThat(orderItem.getTotalPrice()).isEqualTo(Money.of(12000));
    }
}
