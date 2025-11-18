package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.command.OrderLineCommand;
import com.loopers.domain.order.command.OrderPlacementCommand;
import com.loopers.domain.order.service.OrderPlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
@Transactional
public class OrderFacade {

    private final OrderPlacementService orderPlacementService;

    public OrderInfo placeOrder(OrderCommand command) {
        List<OrderLineCommand> domainOrderLines = command.getOrderLines().stream()
                .map(line -> OrderLineCommand.of(line.getProductId(), line.getQuantity()))
                .toList();

        OrderPlacementCommand domainCommand = OrderPlacementCommand.of(
                command.getMemberId(),
                domainOrderLines
        );

        Order order = orderPlacementService.placeOrder(domainCommand);
        return OrderInfo.from(order);
    }
}