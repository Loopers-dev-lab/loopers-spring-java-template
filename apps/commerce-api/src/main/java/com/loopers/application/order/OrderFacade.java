package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * packageName : com.loopers.application.order
 * fileName     : OrderFacade
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final PointService pointService;

    @Transactional
    public OrderInfo createOrder(CreateOrderCommand command) {

        if (command == null || command.items() == null || command.items().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 정보가 비어있습니다");
        }

        Order order = Order.create(command.userId());

        for (OrderItemCommand itemCommand : command.items()) {

            //상품가져오고
            Product product = productService.getProduct(itemCommand.productId());

            // 재고감소
            product.decreaseStock(itemCommand.quantity());

            // OrderItem생성
            OrderItem orderItem = OrderItem.create(
                    product.getId(),
                    product.getName(),
                    itemCommand.quantity(),
                    product.getPrice());

            order.addOrderItem(orderItem);
            orderItem.setOrder(order);
        }

        //총 가격구하고
        long totalAmount = order.getOrderItems().stream()
                .mapToLong(OrderItem::getAmount)
                .sum();

        order.updateTotalAmount(totalAmount);

        Point point = pointService.findPointByUserId(command.userId());
        point.use(totalAmount);

        //저장
        Order saved = orderService.createOrder(order);
        saved.updateStatus(OrderStatus.COMPLETE);

        return OrderInfo.from(saved);
    }
}
