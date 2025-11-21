package com.loopers.domain.order.dto;

import java.util.List;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 22.
 */
public record OrderCreationResult(
        OrderEntity order,
        List<OrderItemEntity> orderItems
) {}
