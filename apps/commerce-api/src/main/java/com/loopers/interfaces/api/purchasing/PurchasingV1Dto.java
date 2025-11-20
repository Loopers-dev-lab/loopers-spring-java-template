package com.loopers.interfaces.api.purchasing;

import com.loopers.application.purchasing.OrderInfo;
import com.loopers.application.purchasing.OrderItemCommand;
import com.loopers.application.purchasing.OrderItemInfo;
import com.loopers.domain.order.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class PurchasingV1Dto {

    private PurchasingV1Dto() {
    }

    /**
     * 주문 생성 요청 DTO.
     */
    public record CreateRequest(
        @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
        List<@Valid ItemRequest> items
    ) {
        public List<OrderItemCommand> toCommands() {
            return items.stream()
                .map(item -> OrderItemCommand.of(item.productId(), item.quantity()))
                .toList();
        }
    }

    /**
     * 주문 생성 요청 아이템 DTO.
     */
    public record ItemRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @NotNull(message = "상품 수량은 필수입니다.")
        @Min(value = 1, message = "상품 수량은 1개 이상이어야 합니다.")
        Integer quantity
    ) {
    }

    /**
     * 주문 응답 DTO.
     */
    public record OrderResponse(
        Long orderId,
        Long userId,
        Integer totalAmount,
        OrderStatus status,
        List<OrderItemResponse> items
    ) {
        /**
         * OrderInfo로부터 OrderResponse를 생성합니다.
         *
         * @param orderInfo 주문 정보
         * @return 생성된 응답 객체
         */
        public static OrderResponse from(OrderInfo orderInfo) {
            List<OrderItemResponse> itemResponses = orderInfo.items().stream()
                .map(OrderItemResponse::from)
                .toList();

            return new OrderResponse(
                orderInfo.orderId(),
                orderInfo.userId(),
                orderInfo.totalAmount(),
                orderInfo.status(),
                itemResponses
            );
        }
    }

    /**
     * 주문 아이템 응답 DTO.
     */
    public record OrderItemResponse(
        Long productId,
        String name,
        Integer price,
        Integer quantity
    ) {
        /**
         * OrderItemInfo로부터 OrderItemResponse를 생성합니다.
         *
         * @param itemInfo 주문 아이템 정보
         * @return 생성된 응답 객체
         */
        public static OrderItemResponse from(OrderItemInfo itemInfo) {
            return new OrderItemResponse(
                itemInfo.productId(),
                itemInfo.name(),
                itemInfo.price(),
                itemInfo.quantity()
            );
        }
    }

    /**
     * 주문 목록 응답 DTO.
     */
    public record OrdersResponse(List<OrderResponse> orders) {
        /**
         * OrderInfo 목록으로부터 OrdersResponse를 생성합니다.
         *
         * @param orderInfos 주문 정보 목록
         * @return 생성된 응답 객체
         */
        public static OrdersResponse from(List<OrderInfo> orderInfos) {
            return new OrdersResponse(
                orderInfos.stream()
                    .map(OrderResponse::from)
                    .toList()
            );
        }
    }
}


