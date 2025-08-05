package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.embeded.OderUserId;
import com.loopers.domain.order.embeded.OrderNumber;
import com.loopers.domain.order.embeded.OrderStatus;
import com.loopers.domain.order.embeded.OrderTotalPrice;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
public class OrderModel extends BaseEntity {
    
    @Embedded
    private OrderNumber orderNumber;
    
    @Column(name = "user_id", nullable = false)
    private OderUserId userId;
    
    @Embedded
    private OrderStatus status;
    
    @Embedded
    private OrderTotalPrice totalPrice;

    @OneToMany(mappedBy = "orderModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItemModel> orderItems;

    public OrderModel() {

    }

    private OrderModel(OrderNumber orderNumber, OderUserId userId, OrderStatus status, OrderTotalPrice totalPrice, List<OrderItemModel> orderItems) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.orderItems = orderItems;
    }

    public static OrderModel of(String orderNumber, Long userId, String status, BigDecimal totalPrice, List<OrderItemModel> orderItems) {
        return new OrderModel(
                OrderNumber.of(orderNumber),
                OderUserId.of(userId),
                OrderStatus.of(status),
                OrderTotalPrice.of(totalPrice),
                orderItems
        );
    }
    public static OrderModel register(Long userId) {
        return new OrderModel(
                OrderNumber.generate(userId),
                OderUserId.of(userId),
                OrderStatus.pendingPayment(),
                OrderTotalPrice.of(new BigDecimal(BigInteger.ZERO)),
                new ArrayList<>()
        );
    }

    public void addItem(Long productId, Long optionId, BigDecimal quantity, BigDecimal pricePerUnit, String productName,
                        String optionName, String imageUrl) {
        OrderItemModel item =
                OrderItemModel.of(this, productId, optionId,
                        quantity, pricePerUnit, productName, optionName, imageUrl);
        this.orderItems.add(item);
        recalculateTotal();
    }
    public void cancel() {
        this.status = this.status.cancel();
    }
    
    public void updateStatus(String status) {
        this.status = this.status.updateStatus(status);
    }
    
    public boolean canBeCancelled() {
        return this.status.canBeCancelled();
    }
    
    public boolean isPendingPayment() {
        return this.status.isPendingPayment();
    }
    
    public boolean belongsToUser(Long userId) {
        return this.userId.getValue().equals(userId);
    }
    
    public BigDecimal calculateTotal() {
        return orderItems.stream()
                .map(OrderItemModel::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private void recalculateTotal() {
        BigDecimal newTotal = calculateTotal();
        this.totalPrice = OrderTotalPrice.of(newTotal);
    }

}
