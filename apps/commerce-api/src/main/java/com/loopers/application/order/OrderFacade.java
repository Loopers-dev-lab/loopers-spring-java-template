package com.loopers.application.order;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.domain.user.UserId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public OrderInfo getOrder(Long id) {
        OrderModel order = orderService.getOrder(id);
        if (order == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }
        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getUserOrders(UserId userId) {
        UserModel user = userService.getUser(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        List<OrderModel> orders = orderService.getUserOrders(user);
        return orders.stream()
            .map(OrderInfo::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public OrderInfo createOrder(UserId userId, List<OrderService.OrderItemRequest> items) {
        UserModel user = userService.getUser(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        
        OrderModel order = orderService.createOrder(user, items);
        return OrderInfo.from(order);
    }
}
