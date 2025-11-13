package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderTest {

    @DisplayName("주문 생성 시 주문 상태는 INIT(주문생성) 이다.")
    @Test
    void createOrderWithExisistProduct_returnOrder() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getPoint()).thenReturn(BigDecimal.valueOf(100000));

        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(BigDecimal.valueOf(25000));
        when(mockProduct.getStock()).thenReturn(100);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(mockProduct, 1);

        // when
        Order order = Order.createOrder(mockUser, productQuantities);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.INIT);
    }

    @DisplayName("주문 생성 시 상품 리스트와 수량에서 주문의 총 금액을 계산한다.")
    @Test
    void createOrderWithExisistProduct_returnTotalPrice() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getPoint()).thenReturn(BigDecimal.valueOf(100000));

        Product mockProduct1 = mock(Product.class);
        when(mockProduct1.getPrice()).thenReturn(BigDecimal.valueOf(25000));
        when(mockProduct1.getStock()).thenReturn(100);

        Product mockProduct2 = mock(Product.class);
        when(mockProduct2.getPrice()).thenReturn(BigDecimal.valueOf(10000));
        when(mockProduct2.getStock()).thenReturn(100);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(mockProduct1, 1); // 25000 * 1 = 25000
        productQuantities.put(mockProduct2, 2); // 10000 * 2 = 20000

        // when
        Order order = Order.createOrder(mockUser, productQuantities);

        // then
        assertThat(order.getTotalPrice()).isEqualTo(BigDecimal.valueOf(45000));
    }

    @DisplayName("생성된 주문의 상태를 변경할 수 있다.")
    @Test
    void changeOrderStatus_success() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getPoint()).thenReturn(BigDecimal.valueOf(100000));

        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(BigDecimal.valueOf(25000));
        when(mockProduct.getStock()).thenReturn(100);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(mockProduct, 1);

        Order order = Order.createOrder(mockUser, productQuantities);

        // when
        order.updateStatus(OrderStatus.COMPLETED);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @DisplayName("주문 생성 시 사용자는 필수다.")
    @Test
    void createOrderWithoutUser_throwException() {
        // given
        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(BigDecimal.valueOf(25000));

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(mockProduct, 1);

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            Order.createOrder(null, productQuantities)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("사용자는 필수입니다");
    }

    @DisplayName("주문 생성 시 상품은 필수다.")
    @Test
    void createOrderWithoutProducts_throwException() {
        // given
        User mockUser = mock(User.class);

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            Order.createOrder(mockUser, null)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("주문 상품은 필수입니다");
    }

    @DisplayName("주문 생성 시 상품 목록이 비어있으면 예외가 발생한다.")
    @Test
    void createOrderWithEmptyProducts_throwException() {
        // given
        User mockUser = mock(User.class);
        Map<Product, Integer> emptyProducts = new HashMap<>();

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            Order.createOrder(mockUser, emptyProducts)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("주문 상품은 필수입니다");
    }

    @DisplayName("주문 상태 변경 시 상태는 필수다.")
    @Test
    void updateOrderStatusWithNull_throwException() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getPoint()).thenReturn(BigDecimal.valueOf(100000));

        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(BigDecimal.valueOf(25000));
        when(mockProduct.getStock()).thenReturn(100);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(mockProduct, 1);

        Order order = Order.createOrder(mockUser, productQuantities);

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            order.updateStatus(null)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).isEqualTo("주문 상태는 필수입니다");
    }

    @DisplayName("주문 생성 시 재고가 부족하면 예외가 발생한다.")
    @Test
    void createOrder_withInsufficientStock_throwException() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getPoint()).thenReturn(BigDecimal.valueOf(500000)); // 충분한 포인트

        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(BigDecimal.valueOf(25000));
        when(mockProduct.getStock()).thenReturn(5); // 재고 5개
        when(mockProduct.getProductName()).thenReturn("테스트상품");

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(mockProduct, 10); // 10개 주문 (250000원)

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            Order.createOrder(mockUser, productQuantities)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).contains("재고가 부족합니다");
    }

    @DisplayName("주문 생성 시 포인트가 부족하면 예외가 발생한다.")
    @Test
    void createOrder_withInsufficientPoint_throwException() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getPoint()).thenReturn(BigDecimal.valueOf(10000)); // 포인트 10000

        Product mockProduct = mock(Product.class);
        when(mockProduct.getPrice()).thenReturn(BigDecimal.valueOf(25000));
        when(mockProduct.getStock()).thenReturn(100);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(mockProduct, 1); // 25000원 필요

        // when // then
        CoreException exception = assertThrows(CoreException.class, () ->
            Order.createOrder(mockUser, productQuantities)
        );

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(exception.getCustomMessage()).contains("포인트가 부족합니다");
    }

}
