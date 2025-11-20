package com.loopers.application.order;

import com.loopers.application.orderitem.OrderItemInfo;

import java.util.List;

public record OrderResultInfo(
        OrderInfo orderInfo,
        List<OrderItemInfo> orderItemInfos
) {}
