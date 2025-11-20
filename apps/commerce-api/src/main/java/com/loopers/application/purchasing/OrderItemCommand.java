package com.loopers.application.purchasing;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

/**
 * 주문 생성 요청 아이템 명령.
 *
 * @param productId 상품 ID
 * @param quantity 수량
 * @param couponCode 쿠폰 코드 (선택)
 */
public record OrderItemCommand(Long productId, Integer quantity, String couponCode) {
    public OrderItemCommand {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 수량은 1개 이상이어야 합니다.");
        }
    }

    /**
     * Create an OrderItemCommand with no coupon code.
     *
     * @param productId the product identifier
     * @param quantity the quantity of the product
     * @return the created OrderItemCommand with `couponCode` set to `null`
     */
    public static OrderItemCommand of(Long productId, Integer quantity) {
        return new OrderItemCommand(productId, quantity, null);
    }
}
