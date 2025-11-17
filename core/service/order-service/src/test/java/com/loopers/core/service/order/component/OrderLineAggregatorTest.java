package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderItemId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.product.vo.ProductId;
import org.instancio.Instancio;
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
import static org.instancio.Select.field;
import static org.mockito.Mockito.when;

@DisplayName("OrderLineAggregator 단위 테스트")
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
            OrderItem product1 = Instancio.of(OrderItem.class)
                    .set(field(OrderItem::getId), OrderItemId.empty())
                    .set(field(OrderItem::getProductId), new ProductId("1"))
                    .set(field(OrderItem::getQuantity), new Quantity(2L))
                    .set(field(OrderItem::getOrderId), OrderId.empty())
                    .create();
            OrderItem product2 = Instancio.of(OrderItem.class)
                    .set(field(OrderItem::getId), OrderItemId.empty())
                    .set(field(OrderItem::getProductId), new ProductId("2"))
                    .set(field(OrderItem::getQuantity), new Quantity(3L))
                    .set(field(OrderItem::getOrderId), OrderId.empty())
                    .create();
            List<OrderItem> orderItems = List.of(product1, product2);

            when(orderLineAllocator.allocate(product1)).thenReturn(new BigDecimal("20000"));
            when(orderLineAllocator.allocate(product2)).thenReturn(new BigDecimal("30000"));

            // when
            PayAmount result = orderLineAggregator.aggregate(orderItems);

            // then
            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("단일 상품의 가격을 반환한다")
        void aggregateSingleProduct() {
            // given
            OrderItem orderItem = Instancio.of(OrderItem.class)
                    .set(field(OrderItem::getId), OrderItemId.empty())
                    .set(field(OrderItem::getProductId), new ProductId("1"))
                    .set(field(OrderItem::getQuantity), new Quantity(5L))
                    .set(field(OrderItem::getOrderId), OrderId.empty())
                    .create();
            List<OrderItem> orderItems = List.of(orderItem);

            when(orderLineAllocator.allocate(orderItem)).thenReturn(new BigDecimal("50000"));

            // when
            PayAmount result = orderLineAggregator.aggregate(orderItems);

            // then
            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("소수점이 포함된 가격을 정확하게 합산한다")
        void aggregateWithDecimalPrices() {
            // given
            OrderItem product1 = Instancio.of(OrderItem.class)
                    .set(field(OrderItem::getId), OrderItemId.empty())
                    .set(field(OrderItem::getProductId), new ProductId("1"))
                    .set(field(OrderItem::getQuantity), new Quantity(1L))
                    .set(field(OrderItem::getOrderId), OrderId.empty())
                    .create();
            OrderItem product2 = Instancio.of(OrderItem.class)
                    .set(field(OrderItem::getId), OrderItemId.empty())
                    .set(field(OrderItem::getProductId), new ProductId("1"))
                    .set(field(OrderItem::getQuantity), new Quantity(2L))
                    .set(field(OrderItem::getOrderId), OrderId.empty())
                    .create();
            List<OrderItem> orderItems = List.of(product1, product2);

            when(orderLineAllocator.allocate(product1)).thenReturn(new BigDecimal("10000.50"));
            when(orderLineAllocator.allocate(product2)).thenReturn(new BigDecimal("20000.75"));

            // when
            PayAmount result = orderLineAggregator.aggregate(orderItems);

            // then
            assertThat(result.value()).isEqualByComparingTo(new BigDecimal("30001.25"));
        }
    }
}
