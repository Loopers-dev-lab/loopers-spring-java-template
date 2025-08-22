package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.order.embeded.OrderUserId;
import com.loopers.domain.order.embeded.OrderNumber;
import com.loopers.domain.order.embeded.OrderStatus;
import com.loopers.domain.order.embeded.OrderTotalPrice;
import com.loopers.domain.order.item.OrderItemModel;
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
    @Embedded private OrderNumber orderNumber;
    @Embedded private OrderUserId userId;
    @Embedded private OrderStatus status;
    @Embedded private OrderTotalPrice totalPrice;

    @OneToMany(cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private final List<OrderItemModel> orderItems = new ArrayList<>();


    public OrderModel() {
    }

    private OrderModel(OrderNumber orderNumber, OrderUserId userId, OrderStatus status, OrderTotalPrice totalPrice) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public static OrderModel of(String orderNumber, Long userId, String status, BigDecimal totalPrice) {
        return new OrderModel(
                OrderNumber.of(orderNumber),
                OrderUserId.of(userId),
                OrderStatus.of(status),
                OrderTotalPrice.of(totalPrice)
        );
    }
    public static OrderModel register(Long userId) {
        return new OrderModel(
                OrderNumber.generate(userId),
                OrderUserId.of(userId),
                OrderStatus.pendingPayment(),
                OrderTotalPrice.of(new BigDecimal(BigInteger.ZERO))
        );
    }

    public void addItem(Long productId, Long optionId, int quantity, BigDecimal pricePerUnit,
                        String productName, String optionName, String imageUrl) {
        OrderItemModel item = OrderItemModel.of(
                this.getId(), productId, optionId, quantity, pricePerUnit, productName, optionName, imageUrl
        );
        this.orderItems.add(item);
        recalcTotal();
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

    private void recalcTotal() {
        this.totalPrice = OrderTotalPrice.of(calculateTotal());
    }

    public void applyFixedCoupon(CouponModel couponModel) {
        if (couponModel.getType().isFixed()) {
            BigDecimal discountAmount = couponModel.getValue().getValue();
            if (discountAmount.compareTo(this.totalPrice.getValue()) > 0) {
                throw new IllegalArgumentException("쿠폰 할인 금액이 주문 총액보다 큽니다.");
            }
            this.totalPrice = this.totalPrice.subtract(discountAmount);
        } else {
            throw new IllegalArgumentException("유효하지 않은 쿠폰 타입입니다.");
        }
    }

    public void applyRateCoupon(CouponModel couponModel) {
        if (couponModel.getType().isRate()) {
            BigDecimal rate = couponModel.getValue().getValue();
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                throw new IllegalArgumentException("할인률은 0 이상 1 미만이어야 합니다.");
            }
            BigDecimal discountAmount = this.totalPrice.getValue().multiply(rate);
            this.totalPrice = this.totalPrice.applyRate(discountAmount);
        } else {
            throw new IllegalArgumentException("유효하지 않은 쿠폰 타입입니다.");
        }
    }
}
