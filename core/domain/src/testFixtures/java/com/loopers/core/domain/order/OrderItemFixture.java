package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderItemId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.ProductId;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class OrderItemFixture {

    public static OrderItem create() {
        return Instancio.of(OrderItem.class)
                .set(field(OrderItem::getId), OrderItemId.empty())
                .set(field(OrderItem::getOrderId), Instancio.create(OrderId.class))
                .set(field(OrderItem::getProductId), Instancio.create(ProductId.class))
                .set(field(OrderItem::getQuantity), new Quantity(1L))
                .create();
    }

    public static OrderItem createWith(OrderId orderId, ProductId productId) {
        return Instancio.of(OrderItem.class)
                .set(field(OrderItem::getId), OrderItemId.empty())
                .set(field(OrderItem::getOrderId), orderId)
                .set(field(OrderItem::getProductId), productId)
                .set(field(OrderItem::getQuantity), new Quantity(1L))
                .create();
    }

    public static OrderItem createWith(OrderId orderId, ProductId productId, long quantity) {
        return Instancio.of(OrderItem.class)
                .set(field(OrderItem::getId), OrderItemId.empty())
                .set(field(OrderItem::getOrderId), orderId)
                .set(field(OrderItem::getProductId), productId)
                .set(field(OrderItem::getQuantity), new Quantity(quantity))
                .create();
    }
}
