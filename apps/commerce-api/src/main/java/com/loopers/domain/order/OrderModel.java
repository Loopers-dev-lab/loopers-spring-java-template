package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.common.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;

import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "orders")
public class OrderModel extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserModel user;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "total_price"))
    private Money totalPrice;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order")
    private List<OrderItemModel> orderItems = new ArrayList<>();

    public OrderModel() {
    }

    public OrderModel(UserModel user, Money totalPrice, List<OrderItemModel> orderItems) {
        this.user = user;
        this.totalPrice = totalPrice;
        this.orderItems = orderItems;
    }

    public UserModel getUser() {
        return user;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public List<OrderItemModel> getOrderItems() {
        return orderItems;
    }

    public void addOrderItem(OrderItemModel orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
}
