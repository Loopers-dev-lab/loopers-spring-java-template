package com.loopers.application.order;

import com.loopers.domain.order.PaymentMethod;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OrderRequest(
        List<OrderItemRequest> items,
        Long couponId,
        PaymentRequest paymentRequest,
        PaymentMethod paymentMethod
) {
    public OrderRequest(List<OrderItemRequest> items) {
        this(items, null, null, PaymentMethod.POINT);
    }

    public OrderRequest(List<OrderItemRequest> items, Long couponId) {
        this(items, couponId, null, PaymentMethod.POINT);
    }

    public Map<Long, Integer> toItemQuantityMap() {
        return items.stream().collect(Collectors.toMap(
                OrderItemRequest::productId,
                OrderItemRequest::quantity)
        );
    }

    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.");
        }
        for (OrderItemRequest item : items) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "각 주문 항목의 수량은 1 이상이어야 합니다.");
            }
        }
        if (items.stream().map(OrderItemRequest::productId).distinct().count() < items.size()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "중복된 상품ID가 존재합니다.");
        }
        if (couponId != null && couponId < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "잘못된 쿠폰ID입니다.");
        }
        if (paymentMethod == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 수단은 필수입니다.");
        }
        if (paymentMethod == PaymentMethod.PG && paymentRequest == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "PG 결제 시 결제 정보가 필요합니다.");
        }
        if (paymentMethod == PaymentMethod.POINT && paymentRequest != null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 결제에서는 결제 정보가 필요하지 않습니다.");
        }
    }

    public record PaymentRequest(
            String cardType,
            String cardNo
    ) {
    }
}
