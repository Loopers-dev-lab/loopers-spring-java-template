package com.loopers.interfaces.api.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemInfo;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public class OrderCreateV1Dto {
  public record OrderItemRequest(long productId, long quantity) {
  }

  public record OrderRequest(List<OrderItemRequest> items) {
  }

  public record OrderResponse(long id, String status,
                              @JsonSerialize(using = ToStringSerializer.class) BigDecimal paymentPrice
      , @JsonSerialize(using = ToStringSerializer.class) BigDecimal totalPrice
      , @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul") ZonedDateTime orderAt
      , List<OrderItemInfo> orderItemInfo) {
    public static OrderResponse from(OrderInfo info) {
      if (info == null) return null;
      return new OrderResponse(
          info.id(),
          info.status(),
          info.paymentPrice(),
          info.totalPrice(),
          info.orderAt(),
          info.orderItemInfo()
      );
    }
  }
}
