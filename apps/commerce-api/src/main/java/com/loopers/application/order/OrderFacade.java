package com.loopers.application.order;

import com.loopers.application.event.order.OrderPlacedEvent;
import com.loopers.application.event.tracking.UserActionEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.command.OrderLineCommand;
import com.loopers.domain.order.command.OrderPlacementCommand;
import com.loopers.domain.order.service.OrderPlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@RequiredArgsConstructor
@Component
@Transactional
public class OrderFacade {

    private final OrderPlacementService orderPlacementService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderInfo placeOrder(OrderCommand command) {
        List<OrderLineCommand> domainOrderLines = command.getOrderLines().stream()
                .map(line -> OrderLineCommand.of(line.getProductId(), line.getQuantity()))
                .toList();

        OrderPlacementCommand domainCommand = OrderPlacementCommand.of(
                command.getMemberId(),
                domainOrderLines,
                command.getMemberCouponId()
        );

        Order order = orderPlacementService.placeOrder(domainCommand);

        // 주문 생성 이벤트 발행
        eventPublisher.publishEvent(new OrderPlacedEvent(
            order.getOrderNo(),
            order.getMemberId(),
            command.getMemberCouponId(),
            order.getTotalPrice(),
            LocalDateTime.now()
        ));

        eventPublisher.publishEvent(UserActionEvent.of(
            "ORDER_PLACED",
            order.getMemberId(),
            "ORDER",
            order.getOrderNo(),
            Map.of("itemCount", order.getItems().size(), "totalPrice", order.getTotalPrice().getAmount())
        ));

        return OrderInfo.from(order);
    }
}
