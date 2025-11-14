package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * 주문 생성
     */
    @Transactional
    public OrderInfo createOrder(String loginId, List<OrderService.OrderItemRequest> orderItemRequests) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[loginId = " + loginId + "] User를 찾을 수 없습니다."
                ));

        Order order = orderService.createOrder(user, orderItemRequests);
        return OrderInfo.from(order);
    }

    /**
     * 단일 주문 상세 조회
     */
    public OrderInfo getOrder(Long orderId) {
        Order order = orderService.findOrderById(orderId);
        return OrderInfo.from(order);
    }

    /**
     * 유저의 주문 목록 조회
     */
    public List<OrderInfo> getOrders(String loginId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[loginId = " + loginId + "] User를 찾을 수 없습니다."
                ));

        List<Order> orders = orderService.findOrdersByUser(user);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }
}

