package com.loopers.application.order;

import com.loopers.domain.order.Delivery;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final PointService pointService;

    @Transactional
    public OrderInfo createOrder(OrderCommand.CreateOrderCommand createOrderCommand) {
        User user = userService.findUserByLoginId(createOrderCommand.loginId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Delivery delivery = OrderCommand.DeliveryCommand.toDelivery(createOrderCommand.delivery());
        Order order = orderService.createOrder(user.getId(), delivery);

        int totalPrice = 0;
        for (OrderCommand.OrderItemCommand orderItemCommand : createOrderCommand.orderItems()) {
            Product product = productService.findProductById(orderItemCommand.productId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            orderService.createOrderItem(order, product, orderItemCommand.quantity());
            totalPrice += orderService.addTotalPrice(order, product.getPrice().getPrice(), orderItemCommand.quantity());

            productService.reduceStock(product.getId(), orderItemCommand.quantity());
        }

        pointService.deductPoint(user.getId(), totalPrice);

        orderService.saveOrder(order);
        return OrderInfo.from(order, order.getOrderItems(), delivery);
    }

    @Transactional
    public List<OrderInfo> getOrdersInfo(String loginId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        List<Order> orders = orderService.findOrdersByUserId(user.getId());

        return orders.stream()
                .map(order -> OrderInfo.from(order, order.getOrderItems(), order.getDelivery()))
                .toList();
    }

    @Transactional
    public OrderInfo getOrderInfo(String loginId, Long orderId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Order order = orderService.findOrderByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        return OrderInfo.from(order, order.getOrderItems(), order.getDelivery());
    }
}
