package com.loopers.core.service.order;

import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.common.vo.PageNo;
import com.loopers.core.domain.common.vo.PageSize;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.order.query.GetOrderListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderListView getOrderListWithCondition(GetOrderListQuery query) {
        User user = userRepository.getByIdentifier(new UserIdentifier(query.getUserIdentifier()));

        return orderRepository.findListWithCondition(
                user.getUserId(),
                OrderSort.from(query.getCreatedAtSort()),
                new PageNo(query.getPageNo()),
                new PageSize(query.getPageSize())
        );
    }
}
