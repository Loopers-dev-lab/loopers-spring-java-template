package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderResultInfo;
import com.loopers.application.orderitem.OrderItemInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.orderitem.OrderItem;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public class OrderV1Dto {
    public record OrderResponse(
            Long id,
            Long userId,
            OrderStatus status,
            BigDecimal totalPrice,
            List<OrderItemResponse> orderItems,
            ZonedDateTime createdAt
    )
    {
        public static OrderResponse from(OrderResultInfo resultInfo) {
            OrderInfo orderInfo = resultInfo.orderInfo();
            List<OrderItemInfo> orderItemInfos = resultInfo.orderItemInfos();

            return new OrderResponse(
                    orderInfo.id(),
                    orderInfo.userId(),
                    orderInfo.orderStatus(),
                    orderInfo.totalPrice(),
                    orderItemInfos.stream()
                                    .map(itemInfo -> new OrderItemResponse(
                                            itemInfo.id(),
                                            itemInfo.productId(),
                                            itemInfo.quantity(),
                                            itemInfo.orderPrice()
                                    ))
                                    .toList(),
                    orderInfo.createdAt()
            );
        }
    }

    public record OrderItemResponse(
            Long id,
            Long productId,
            int quantity,
            BigDecimal orderPrice
    ) {}



    public record OrderRequest(
            Long userId,
            List<OrderItemRequest> orderItems
    ) {
        public Order toEntity(BigDecimal totalPrice) {
            return new Order(
                    userId,
                    totalPrice,
                    OrderStatus.READY
            );
        }
    }

    public record OrderItemRequest(Long productId, int quantity) {
        public OrderItem toEntity(Long orderId, BigDecimal orderPrice) {
            return new OrderItem(
                    orderId,
                    productId,
                    quantity,
                    orderPrice
            );
        }
    }
}
