package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class OrderDto {

    @Builder
    public record OrderItemRequest(
            Long productId,
            Integer quantity
    ) {}

    @Builder
    public record CreateOrderRequest(
            List<OrderItemRequest> items
            , List<Long> couponIds  // 쿠폰 ID 리스트
            , PaymentDto.PaymentMethod paymentMethod  // 결제 방법 (기본값: CARD)
    ) {
        /**
         * 주문 요청 유효성 검사
         */
        public void validate() {
            if (items() == null || items().isEmpty()) {
                throw new CoreException(
                        ErrorType.BAD_REQUEST,
                        "주문 항목이 1개 이상이어야 합니다."
                );
            }
        }
        
        /**
         * 결제 방법 반환 (null인 경우 기본값 CARD)
         */
        public PaymentDto.PaymentMethod getPaymentMethod() {
            return paymentMethod != null ? paymentMethod : PaymentDto.PaymentMethod.CARD;
        }

        /**
         * 멱등성 키 생성
         */
        public String generateIdempotentKey(Long userId) {
                // items와 couponIds를 기반으로 해시 생성
                String itemsString = items().stream()
                        .map(item -> item.productId() + ":" + item.quantity())
                        .sorted()
                        .collect(Collectors.joining(","));
                String couponString = couponIds() != null 
                        ? couponIds().stream().map(String::valueOf).sorted().collect(Collectors.joining(","))
                        : "";
                return userId + ":" + itemsString + ":" + couponString;
        }
    }

    @Builder
    public record OrderResponse(
            Long id,
            BigDecimal finalAmount,
            BigDecimal totalPrice,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            String orderStatus,
            String userLoginId,
            List<OrderItemResponse> orderItems
    ) {
        public static OrderResponse from(OrderInfo orderInfo) {
            return OrderResponse.builder()
                    .id(orderInfo.id())
                    .finalAmount(orderInfo.finalAmount())
                    .totalPrice(orderInfo.totalPrice())
                    .discountAmount(orderInfo.discountAmount())
                    .shippingFee(orderInfo.shippingFee())
                    .orderStatus(orderInfo.orderStatus().name())
                    .userLoginId(orderInfo.userLoginId())
                    .orderItems(orderInfo.orderItems().stream()
                            .map(OrderItemResponse::from)
                            .toList())
                    .build();
        }
    }

    @Builder
    public record OrderItemResponse(
            Long id,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            Long productId,
            String productName
    ) {
        public static OrderItemResponse from(OrderInfo.OrderItemInfo orderItemInfo) {
            return OrderItemResponse.builder()
                    .id(orderItemInfo.id())
                    .quantity(orderItemInfo.quantity())
                    .unitPrice(orderItemInfo.unitPrice())
                    .totalAmount(orderItemInfo.totalAmount())
                    .productId(orderItemInfo.productId())
                    .productName(orderItemInfo.productName())
                    .build();
        }
    }

}

