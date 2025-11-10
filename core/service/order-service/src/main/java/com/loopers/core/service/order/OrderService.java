package com.loopers.core.service.order;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderedProduct;
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
    private final OrderLineAggregator orderLineAggregator;
    private final OrderCashier orderCashier;

    @Transactional
    public Order order(OrderProductsCommand command) {
        List<OrderProductsCommand.OrderProduct> orderedProductCommands = command.getProducts();
        List<OrderedProduct> orderedProducts = orderedProductCommands.stream()
                .map(productCommand -> new OrderedProduct(
                                new ProductId(productCommand.getProductId()),
                                new Quantity(productCommand.getQuantity())
                        )
                )
                .toList();

        PayAmount payAmount = orderLineAggregator.aggregate(orderedProducts);
        User user = userRepository.getByIdentifier(new UserIdentifier(command.getUserIdentifier()));

        return orderCashier.checkout(user, Order.create(user.getUserId(), orderedProducts), payAmount);
    }
}
