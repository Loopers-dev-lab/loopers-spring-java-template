package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.OrderedProduct;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.product.vo.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("주문 라인 집계기")
@ExtendWith(MockitoExtension.class)
class OrderLineAggregatorTest {

    @Mock
    private OrderLineAllocator orderLineAllocator;

    @InjectMocks
    private OrderLineAggregator orderLineAggregator;

    @Nested
    @DisplayName("aggregate 메서드")
    class AggregateMethod {

        @Test
        @DisplayName("여러 상품의 가격을 합산한다")
        void aggregateMultipleProducts() {
            // given
            OrderedProduct product1 = new OrderedProduct(new ProductId("1"), new Quantity(2L));
            OrderedProduct product2 = new OrderedProduct(new ProductId("2"), new Quantity(3L));
            List<OrderedProduct> orderedProducts = List.of(product1, product2);

            when(orderLineAllocator.allocate(product1)).thenReturn(new BigDecimal("20000"));
            when(orderLineAllocator.allocate(product2)).thenReturn(new BigDecimal("30000"));

            // when
            PayAmount result = orderLineAggregator.aggregate(orderedProducts);

            // then
            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("단일 상품의 가격을 반환한다")
        void aggregateSingleProduct() {
            // given
            OrderedProduct orderedProduct = new OrderedProduct(new ProductId("1"), new Quantity(5L));
            List<OrderedProduct> orderedProducts = List.of(orderedProduct);

            when(orderLineAllocator.allocate(orderedProduct)).thenReturn(new BigDecimal("50000"));

            // when
            PayAmount result = orderLineAggregator.aggregate(orderedProducts);

            // then
            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("소수점이 포함된 가격을 정확하게 합산한다")
        void aggregateWithDecimalPrices() {
            // given
            OrderedProduct product1 = new OrderedProduct(new ProductId("1"), new Quantity(1L));
            OrderedProduct product2 = new OrderedProduct(new ProductId("2"), new Quantity(1L));
            List<OrderedProduct> orderedProducts = List.of(product1, product2);

            when(orderLineAllocator.allocate(product1)).thenReturn(new BigDecimal("10000.50"));
            when(orderLineAllocator.allocate(product2)).thenReturn(new BigDecimal("20000.75"));

            // when
            PayAmount result = orderLineAggregator.aggregate(orderedProducts);

            // then
            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("30001.25"));
        }
    }
}
