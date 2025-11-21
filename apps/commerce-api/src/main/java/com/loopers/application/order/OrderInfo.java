package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record OrderInfo(long id, String status, BigDecimal totalPrice
    , ZonedDateTime orderAt, List<OrderItemInfo> orderItemInfo) {
  public static OrderInfo from(Order model) {
    if (model == null) throw new CoreException(ErrorType.NOT_FOUND, "주문정보를 찾을수 없습니다.");
    return new OrderInfo(
        model.getId(),
        model.getStatus().name(),
        model.getTotalPrice().getAmount(),
        model.getOrderAt(),
        OrderItemInfo.from(model.getOrderItems())
    );
  }
}
