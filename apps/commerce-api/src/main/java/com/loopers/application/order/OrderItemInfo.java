package com.loopers.application.order;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.loopers.domain.order.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemInfo(Long id, long quantity
    , @JsonSerialize(using = ToStringSerializer.class) BigDecimal unitPrice,
                            @JsonSerialize(using = ToStringSerializer.class) BigDecimal totalPrice) {
  public static List<OrderItemInfo> from(List<OrderItem> model) {
    if (model == null) throw new CoreException(ErrorType.NOT_FOUND, "주문상세정보를 찾을수 없습니다.");
    return model.stream().map(item -> new OrderItemInfo(
        item.getId(),
        item.getQuantity(),
        item.getUnitPrice().getAmount(),
        item.getTotalPrice().getAmount()
    )).toList();
  }
}
