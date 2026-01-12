package com.loopers.domain.order;

import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderItemModelTest {
    @DisplayName("주문 항목 모델 생성")
    @Nested
    class Create {
        
        @DisplayName("주문 항목이 정상적으로 생성된다")
        @Test
        void createsOrderItem_whenValidParameters() {
            // arrange
            ProductModel product = new ProductModel("product1", new Brand("Apple"), new Money(10000), new Quantity(10));
            Quantity quantity = new Quantity(3);
            Money orderPrice = new Money(30000);

            // act
            OrderItemModel orderItem = new OrderItemModel(product, quantity, orderPrice);

            // assert
            assertAll(
                () -> assertThat(orderItem).isNotNull(),
                () -> assertThat(orderItem.getProduct()).isEqualTo(product),
                () -> assertThat(orderItem.getQuantity()).isEqualTo(quantity),
                () -> assertThat(orderItem.getOrderPrice()).isEqualTo(orderPrice)
            );
        }

        @DisplayName("주문 항목의 가격이 상품 가격과 수량의 곱과 일치한다")
        @Test
        void createsOrderItem_withCorrectPriceCalculation() {
            // arrange
            ProductModel product = new ProductModel("product1", new Brand("Apple"), new Money(10000), new Quantity(10));
            Quantity quantity = new Quantity(2);
            Money expectedOrderPrice = new Money(20000); // 10000 * 2

            // act
            OrderItemModel orderItem = new OrderItemModel(product, quantity, expectedOrderPrice);

            // assert
            assertThat(orderItem.getOrderPrice().value()).isEqualTo(expectedOrderPrice.value());
        }
    }
}

