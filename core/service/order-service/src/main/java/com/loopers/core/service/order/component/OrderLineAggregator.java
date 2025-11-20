package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.payment.vo.PayAmount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderLineAggregator {

    private final OrderLineAllocator allocator;

    public PayAmount aggregate(List<OrderItem> orderItems) {
        return new PayAmount(
                orderItems.stream()
                        .map(allocator::allocate)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }
}
