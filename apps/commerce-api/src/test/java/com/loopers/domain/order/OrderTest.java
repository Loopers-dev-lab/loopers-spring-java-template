package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @DisplayName("주문 생성 시 주문 상태는 INIT(주문생성) 이다.")
    @Test
    void createOrderWithExisistProduct_returnOrder() {

        List<Product> products = List.of(
                Product.builder()
                        .productCode("P001")
                        .productName("상품1")
                        .price(BigDecimal.valueOf(25000))
                        .build()
        );

        Order order = Order.createOrder(products);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INIT);

    }

}
