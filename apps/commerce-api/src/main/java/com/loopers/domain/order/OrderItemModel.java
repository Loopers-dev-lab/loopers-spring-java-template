package com.loopers.domain.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.embeded.*;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter
public class OrderItemModel extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "fk_order_item_order"))
    private OrderModel orderModel;
    
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
    
    protected OrderItemModel() {}

    private OrderItemModel(OrderModel orderModel, OrderItemProductId productId, OrderItemOptionId optionId, OrderItemQuantity quantity, OrderItemPrice orderItemPrice, ProductSnapshot productSnapshot) {
        this.orderModel = orderModel;
        this.productId = productId;
        this.optionId = optionId;
        this.quantity = quantity;
        this.orderItemPrice = orderItemPrice;
        this.productSnapshot = productSnapshot;
    }

    public static OrderItemModel of(OrderModel orderModel, Long productId, Long optionId,
                                    BigDecimal quantity, BigDecimal pricePerUnit,
                                    String productName, String optionName, String imageUrl) {
        return new OrderItemModel(
                orderModel,
                OrderItemProductId.of(productId),
                OrderItemOptionId.of(optionId),
                OrderItemQuantity.of(quantity),
                OrderItemPrice.of(pricePerUnit),
                ProductSnapshot.of(productName, optionName, imageUrl, pricePerUnit)
        );
    }
    public static OrderItemModel ofByOrderItemDetail(OrderModel orderModel, OrderInfo.OrderDetail.OrderItemDetail orderItemDetail) {
        return new OrderItemModel(
                orderModel,
                OrderItemProductId.of(orderItemDetail.productId()),
                OrderItemOptionId.of(orderItemDetail.optionId()),
                OrderItemQuantity.of(orderItemDetail.price()),
                OrderItemPrice.of(orderItemDetail.price()),
                ProductSnapshot.of(orderItemDetail.productName(),
                        orderItemDetail.optionName(),
                        orderItemDetail.productImageUrl(),
                        orderItemDetail.price())
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
        return this.orderItemPrice.getValue().multiply(this.quantity.getValue());
    }
    
    public ProductSnapshot getProductSnapshot() {
        return this.productSnapshot;
    }
    
    public boolean belongsToOrder(Long orderId) {
        return this.orderModel.getId().equals(orderId);
    }

}
