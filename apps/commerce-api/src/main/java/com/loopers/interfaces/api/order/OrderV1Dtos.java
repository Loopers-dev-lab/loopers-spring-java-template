package com.loopers.interfaces.api.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderItemEntity;
import com.loopers.domain.order.OrderStatus;

public class OrderV1Dtos {

    @Schema(description = "주문 등록 요청")
    public record OrderCreateRequest(
            @Schema(description = "주문 상품 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            List<OrderItemRequest> items
    ) {
        public OrderCreateCommand toCommand(String username) {
            List<OrderItemCommand> orderItems = items.stream()
                    .map(item -> new OrderItemCommand(
                            item.productId(),
                            item.quantity(),
                            item.couponId()
                    ))
                    .toList();
            return new OrderCreateCommand(username, orderItems);
        }
    }

    @Schema(description = "주문 상품 정보")
    public record OrderItemRequest(
            @Schema(description = "상품 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Long productId,

            @Schema(description = "수량", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
            Integer quantity,

            @Schema(description = "쿠폰 ID (선택)", example = "1")
            Long couponId
    ) {
    }

    @Schema(description = "주문 등록 응답")
    public record OrderCreateResponse(
            @Schema(description = "주문 ID", example = "1")
            Long orderId,

            @Schema(description = "주문 상태", example = "PENDING")
            OrderStatus status,

            @Schema(description = "할인 전 총액", example = "25000.00")
            BigDecimal originalTotalAmount,

            @Schema(description = "할인 금액", example = "5000.00")
            BigDecimal discountAmount,

            @Schema(description = "할인 후 총액 (실제 결제 금액)", example = "20000.00")
            BigDecimal finalTotalAmount,

            @Schema(description = "주문 일시")
            ZonedDateTime orderedAt
    ) {
        public static OrderCreateResponse from(OrderInfo orderInfo) {
            return new OrderCreateResponse(
                    orderInfo.id(),
                    orderInfo.status(),
                    orderInfo.originalTotalAmount(),
                    orderInfo.discountAmount(),
                    orderInfo.finalTotalAmount(),
                    orderInfo.createdAt()
            );
        }
    }

    @Schema(description = "주문 상세 응답")
    public record OrderDetailResponse(
            @Schema(description = "주문 ID", example = "1")
            Long orderId,

            @Schema(description = "사용자 ID", example = "1")
            Long userId,

            @Schema(description = "주문 상태", example = "PENDING")
            OrderStatus status,

            @Schema(description = "할인 전 총액", example = "25000.00")
            BigDecimal originalTotalAmount,

            @Schema(description = "할인 금액", example = "5000.00")
            BigDecimal discountAmount,

            @Schema(description = "할인 후 총액 (실제 결제 금액)", example = "20000.00")
            BigDecimal finalTotalAmount,

            @Schema(description = "주문 상품 목록")
            List<OrderItemResponse> items,

            @Schema(description = "주문 일시")
            ZonedDateTime orderedAt,

            @Schema(description = "수정 일시")
            ZonedDateTime updatedAt
    ) {
        public static OrderDetailResponse from(OrderInfo orderInfo) {
            return new OrderDetailResponse(
                    orderInfo.id(),
                    orderInfo.userId(),
                    orderInfo.status(),
                    orderInfo.originalTotalAmount(),
                    orderInfo.discountAmount(),
                    orderInfo.finalTotalAmount(),
                    orderInfo.orderItems().stream()
                            .map(OrderItemResponse::from)
                            .toList(),
                    orderInfo.createdAt(),
                    orderInfo.updatedAt()
            );
        }
    }

    @Schema(description = "주문 상품 응답")
    public record OrderItemResponse(
            @Schema(description = "주문 항목 ID", example = "1")
            Long id,

            @Schema(description = "상품 ID", example = "1")
            Long productId,

            @Schema(description = "수량", example = "2")
            Integer quantity,

            @Schema(description = "단가", example = "10000.00")
            BigDecimal unitPrice,

            @Schema(description = "할인 금액", example = "2000.00")
            BigDecimal discountAmount,

            @Schema(description = "총 금액 (할인 적용 후)", example = "18000.00")
            BigDecimal totalPrice
    ) {
        public static OrderItemResponse from(com.loopers.application.order.OrderItemInfo itemInfo) {
            return new OrderItemResponse(
                    itemInfo.id(),
                    itemInfo.productId(),
                    itemInfo.quantity(),
                    itemInfo.unitPrice(),
                    itemInfo.discountAmount(),
                    itemInfo.totalPrice()
            );
        }
    }

    @Schema(description = "주문 목록 응답")
    public record OrderListResponse(
            @Schema(description = "주문 ID", example = "1")
            Long orderId,

            @Schema(description = "주문 상태", example = "PENDING")
            OrderStatus status,

            @Schema(description = "할인 전 총액", example = "25000.00")
            BigDecimal originalTotalAmount,

            @Schema(description = "할인 금액", example = "5000.00")
            BigDecimal discountAmount,

            @Schema(description = "할인 후 총액 (실제 결제 금액)", example = "20000.00")
            BigDecimal finalTotalAmount,

            @Schema(description = "주문 일시")
            ZonedDateTime orderedAt
    ) {
        public static OrderListResponse from(OrderInfo orderInfo) {
            return new OrderListResponse(
                    orderInfo.id(),
                    orderInfo.status(),
                    orderInfo.originalTotalAmount(),
                    orderInfo.discountAmount(),
                    orderInfo.finalTotalAmount(),
                    orderInfo.createdAt()
            );
        }
    }
}
