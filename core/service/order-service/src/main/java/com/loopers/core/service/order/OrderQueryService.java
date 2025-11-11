package com.loopers.core.service.order;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderDetail;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.order.query.GetOrderDetailQuery;
import com.loopers.core.service.order.query.GetOrderListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderListView getOrderListWithCondition(GetOrderListQuery query) {
        User user = userRepository.getByIdentifier(new UserIdentifier(query.getUserIdentifier()));

        return orderRepository.findListWithCondition(
                user.getUserId(),
                OrderSort.from(query.getCreatedAtSort()),
                query.getPageNo(),
                query.getPageSize()
        );
    }

    public OrderDetail getOrderDetail(GetOrderDetailQuery query) {
        Order order = orderRepository.getById(new OrderId(query.getOrderId()));
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getOrderId());

        return new OrderDetail(order, orderItems);
    }
}
