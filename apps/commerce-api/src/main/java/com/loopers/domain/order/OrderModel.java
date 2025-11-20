package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserModel;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderModel extends BaseEntity {

    private Integer orderCnt;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Integer totalPrice;
    private Integer normalPrice;
    private Integer errorPrice;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    /**
     * ORDER ||--|{ ORDER_ITEM : "주문 내용" 은 양방향 관계로 설정합니다.
     */
    @OneToMany(mappedBy = "orders",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<OrderItemModel> orderItems = new ArrayList<>();


    protected OrderModel() {}

    static OrderModel createPending(UserModel userModel, List<OrderItemModel> orderItems) {
        OrderModel orderModel = new OrderModel();
        orderModel.user = userModel;
        orderModel.updateToPending();
        orderModel.orderCnt = orderItems.size();
        orderModel.totalPrice = getTotalPrice(orderItems);
        for(OrderItemModel orderItem : orderItems) {
            orderModel.addItem(orderItem);
        }
        return orderModel;
    }

    static OrderModel createSuccess(UserModel userModel, List<OrderItemModel> orderItems, int normalPrice) {
        OrderModel orderModel = new OrderModel();
        orderModel.user = userModel;
        orderModel.updateToSuccess(normalPrice);
        orderModel.orderCnt = orderItems.size();
        orderModel.totalPrice = getTotalPrice(orderItems);
        for(OrderItemModel orderItem : orderItems) {
            orderModel.addItem(orderItem);
        }
        return orderModel;
    }


    void addItem(OrderItemModel orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrders(this);
    }

    void updateToPending() {
        status = OrderStatus.PENDING;
    }

    void updateToSuccess(int normalPrice) {
        status = OrderStatus.SUCCESS;
        this.normalPrice = normalPrice;
    }

    void updateToFailed(int errorPrice) {
        status = OrderStatus.FAILED;
        this.errorPrice = errorPrice;
    }

    void updateToPartialSuccess(int normalPrice, int errorPrice) {
        status = OrderStatus.PARTIAL_SUCCESS;
        this.normalPrice = normalPrice;
        this.errorPrice = errorPrice;
    }

    static Integer getTotalPrice(List<OrderItemModel> orderItems) {
        Integer totalPrice = 0;
        for (OrderItemModel orderItem : orderItems) {
            totalPrice += orderItem.getPrice();
        }
        return totalPrice;
    }
}
