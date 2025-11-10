package com.loopers.core.service.order;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.order.command.OrderProductsCommand;
import com.loopers.core.service.order.component.OrderCashier;
import com.loopers.core.service.order.component.OrderLineAggregator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderLineAggregator orderLineAggregator;
    private final OrderCashier orderCashier;

    @Transactional
    public Order order(OrderProductsCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.getUserIdentifier()));
        Order savedOrder = orderRepository.save(Order.create(user.getUserId()));
        List<OrderProductsCommand.OrderProduct> orderedProductCommands = command.getProducts();
        List<OrderItem> orderItems = orderedProductCommands.stream()
                .map(productCommand -> OrderItem.create(
                                savedOrder.getOrderId(),
                                new ProductId(productCommand.getProductId()),
                                new Quantity(productCommand.getQuantity())
                        )
                )
                .toList();

        PayAmount payAmount = orderLineAggregator.aggregate(orderItems);
        orderCashier.checkout(user, savedOrder, payAmount);
        
        return savedOrder;
    }
}
