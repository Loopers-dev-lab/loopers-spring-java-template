package com.loopers.domain.order;

import com.loopers.domain.user.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public OrderModel createPendingOrder(UserModel userModel, List<OrderItemModel> orderItems) {
        OrderModel orderModel = OrderModel.createPending(userModel, orderItems);
        return orderRepository.save(orderModel);
    }

    public void updateOrderAsPartialSuccess(OrderModel orderModel, int normalPrice, int errorPrice) {
        orderModel.updateToPartialSuccess(normalPrice, errorPrice);
    }

    public void updateOrderAsSuccess(OrderModel orderModel, int normalPrice) {
        orderModel.updateToSuccess(normalPrice);
    }
}
