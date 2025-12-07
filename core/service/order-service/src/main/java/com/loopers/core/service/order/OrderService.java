package com.loopers.core.service.order;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.order.command.OrderProductsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order order(OrderProductsCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.userIdentifier()));
        Order savedOrder = orderRepository.save(Order.create(user.getId()));
        List<OrderItem> orderItems = command.orderItems().stream()
                .map(productCommand -> {
                            Product product = productRepository.getById(new ProductId(productCommand.productId()));
                            return OrderItem.create(
                                    savedOrder.getId(),
                                    product.getId(),
                                    new Quantity(productCommand.quantity())
                            );
                        }
                )
                .toList();
        orderItemRepository.saveAll(orderItems);

        return savedOrder;
    }
}
