package com.loopers.domain.order.item;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.embeded.ProductSnapshot;
import com.loopers.domain.order.item.embeded.OrderItemOptionId;
import com.loopers.domain.order.item.embeded.OrderItemPrice;
import com.loopers.domain.order.item.embeded.OrderItemProductId;
import com.loopers.domain.order.item.embeded.OrderItemQuantity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter
public class OrderItemModel extends BaseEntity {
    @Embedded
    private OrderModelId orderModelId;
    
    @Embedded
    private OrderItemProductId productId;

    @Embedded
    private OrderItemOptionId optionId;

    @Embedded
    private OrderItemQuantity quantity;
    
    @Embedded
    private OrderItemPrice orderItemPrice;
    
    @Embedded
    private ProductSnapshot productSnapshot;

    public OrderItemModel() {

    }

    private OrderItemModel(OrderModelId orderModelId, OrderItemProductId productId, OrderItemOptionId optionId, OrderItemQuantity quantity, OrderItemPrice orderItemPrice, ProductSnapshot productSnapshot) {
        this.orderModelId = orderModelId;
        this.productId = productId;
        this.optionId = optionId;
        this.quantity = quantity;
        this.orderItemPrice = orderItemPrice;
        this.productSnapshot = productSnapshot;
    }

    public static OrderItemModel of(Long orderModelId, Long productId, Long optionId,
                                    int quantity, BigDecimal pricePerUnit,
                                    String productName, String optionName, String imageUrl) {
        return new OrderItemModel(
                OrderModelId.of(orderModelId),
                OrderItemProductId.of(productId),
                OrderItemOptionId.of(optionId),
                OrderItemQuantity.of(quantity),
                OrderItemPrice.of(pricePerUnit),
                ProductSnapshot.of(productName, optionName, imageUrl, pricePerUnit)
        );
    }
    public void setProductSnapshot(String productName, String optionName, String imageUrl) {
        this.productSnapshot = ProductSnapshot.of(
                productName,
                optionName,
                imageUrl,
                this.orderItemPrice.getValue()
        );
    }
    
    public BigDecimal subtotal() {
        return this.orderItemPrice.getValue().multiply(new BigDecimal(this.quantity.getValue()));
    }
    
    public ProductSnapshot getProductSnapshot() {
        return this.productSnapshot;
    }
    
    public boolean belongsToOrder(Long orderId) {
        return this.orderModelId.value().equals(orderId);
    }

}
