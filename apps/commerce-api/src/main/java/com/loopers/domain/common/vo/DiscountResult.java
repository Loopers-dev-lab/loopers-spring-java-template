package com.loopers.domain.common.vo;


import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public record DiscountResult(
        Long couponId,
        Price originalPrice,
        Price discountAmount,
        Price finalPrice
) {
    public DiscountResult(Price originalPrice) {
        this(null, originalPrice, new Price(0), originalPrice);
    }

    public DiscountResult {
        if (couponId == null && (discountAmount.amount() > 0)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 ID가 없으면 할인 금액이 있을 수 없습니다.");
        }
        if (originalPrice == null || discountAmount == null || finalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격 정보는 null일 수 없습니다.");
        }
        if (originalPrice.amount() != discountAmount.amount() + finalPrice.amount()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격 정보가 일치하지 않습니다.");
        }
    }
}
