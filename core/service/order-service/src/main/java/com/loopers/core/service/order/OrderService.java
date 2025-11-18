package com.loopers.core.service.order;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.CouponId;
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
        User user = userRepository.getByIdentifier(new UserIdentifier(command.userIdentifier()));
        Order savedOrder = orderRepository.save(Order.create(user.getId()));
        CouponId couponId = new CouponId(command.couponId());
        List<OrderItem> orderItems = command.orderItems().stream()
                .map(productCommand -> OrderItem.create(
                                savedOrder.getId(),
                                new ProductId(productCommand.productId()),
                                new Quantity(productCommand.quantity())
                        )
                )
                .toList();

        PayAmount payAmount = orderLineAggregator.aggregate(orderItems);
        orderCashier.checkout(user, savedOrder, payAmount, couponId);

        return savedOrder;
    }
}
