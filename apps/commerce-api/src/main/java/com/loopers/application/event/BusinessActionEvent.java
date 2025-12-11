package com.loopers.application.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BusinessActionEvent(
    Long userId,
    BusinessAction action,
    Long targetId,
    String targetType,
    BigDecimal amount,
    BigDecimal originalAmount,
    String metadata,
    LocalDateTime timestamp
) {
  public enum BusinessAction {
    COUPON_USED,
    PAYMENT_FAILED,
    POINT_EARNED,
    LOYALTY_MILESTONE
  }

  public static BusinessActionEvent couponUsed(Long userId, Long couponId, Long orderId,
                                               BigDecimal originalAmount, BigDecimal discountAmount) {
    return new BusinessActionEvent(
        userId, BusinessAction.COUPON_USED, couponId, "coupon",
        discountAmount, originalAmount, "orderId:" + orderId, LocalDateTime.now()
    );
  }
}
