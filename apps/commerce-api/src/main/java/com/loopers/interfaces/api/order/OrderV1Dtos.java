package com.loopers.interfaces.api.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.domain.order.OrderStatus;

public class OrderV1Dtos {

    @Schema(description = "주문 등록 요청")
    public record OrderCreateRequest(
            @Schema(description = "주문 상품 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            List<OrderItemRequest> items
    ) {
        /**
         * Converts this request into an OrderCreateCommand for the given username.
         *
         * @param username the username of the ordering user
         * @return an OrderCreateCommand containing the provided username and a list of mapped OrderItemCommand instances
         */
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

            @Schema(description = "총 주문 금액", example = "20000.00")
            BigDecimal totalAmount,

            @Schema(description = "주문 일시")
            ZonedDateTime orderedAt
    ) {
        /**
         * Create an API response for a newly created order from the given domain order info.
         *
         * @param orderInfo the domain OrderInfo containing the order's id, status, total amount, and timestamps
         * @return an OrderCreateResponse populated with the order id, status, total amount, and ordered-at timestamp
         */
        public static OrderCreateResponse from(OrderInfo orderInfo) {
            return new OrderCreateResponse(
                    orderInfo.id(),
                    orderInfo.status(),
                    orderInfo.totalAmount(),
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

            @Schema(description = "총 주문 금액", example = "20000.00")
            BigDecimal totalAmount,

            @Schema(description = "주문 상품 목록")
            List<OrderItemResponse> items,

            @Schema(description = "주문 일시")
            ZonedDateTime orderedAt,

            @Schema(description = "수정 일시")
            ZonedDateTime updatedAt
    ) {
        /**
         * Create an API-level OrderDetailResponse from a domain OrderInfo.
         *
         * @param orderInfo the domain object containing full order data to map
         * @return an OrderDetailResponse populated with order id, user id, status, total amount, mapped item list, orderedAt (from createdAt), and updatedAt
         */
        public static OrderDetailResponse from(OrderInfo orderInfo) {
            return new OrderDetailResponse(
                    orderInfo.id(),
                    orderInfo.userId(),
                    orderInfo.status(),
                    orderInfo.totalAmount(),
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

            @Schema(description = "총 금액", example = "20000.00")
            BigDecimal totalPrice
    ) {
        /**
         * Create an API DTO representing a single order item from application-level order item information.
         *
         * @param itemInfo the source OrderItemInfo to convert
         * @return an OrderItemResponse populated with id, productId, quantity, unitPrice, and totalPrice from the source
         */
        public static OrderItemResponse from(com.loopers.application.order.OrderItemInfo itemInfo) {
            return new OrderItemResponse(
                    itemInfo.id(),
                    itemInfo.productId(),
                    itemInfo.quantity(),
                    itemInfo.unitPrice(),
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

            @Schema(description = "총 주문 금액", example = "20000.00")
            BigDecimal totalAmount,

            @Schema(description = "주문 일시")
            ZonedDateTime orderedAt
    ) {
        /**
         * Create an OrderListResponse from an OrderInfo.
         *
         * @param orderInfo the domain order information to map
         * @return the OrderListResponse with orderId, status, totalAmount, and orderedAt
         */
        public static OrderListResponse from(OrderInfo orderInfo) {
            return new OrderListResponse(
                    orderInfo.id(),
                    orderInfo.status(),
                    orderInfo.totalAmount(),
                    orderInfo.createdAt()
            );
        }
    }

    @Schema(description = "페이징 응답")
    public record PageResponse<T>(
            @Schema(description = "데이터 목록")
            List<T> content,

            @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
            int pageNumber,

            @Schema(description = "페이지 크기", example = "20")
            int pageSize,

            @Schema(description = "전체 요소 개수", example = "100")
            long totalElements,

            @Schema(description = "전체 페이지 개수", example = "5")
            int totalPages,

            @Schema(description = "첫 페이지 여부", example = "true")
            boolean first,

            @Schema(description = "마지막 페이지 여부", example = "false")
            boolean last,

            @Schema(description = "비어있는 페이지 여부", example = "false")
            boolean empty
    ) {
        /**
         * Create a PageResponse representing the given Spring Page's content and pagination metadata.
         *
         * @param <T>  the type of content in the page
         * @param page the Spring Page to convert
         * @return a PageResponse containing the page's content, page number, page size, total elements, total pages, and first/last/empty flags
         */
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.isFirst(),
                    page.isLast(),
                    page.isEmpty()
            );
        }
    }
}