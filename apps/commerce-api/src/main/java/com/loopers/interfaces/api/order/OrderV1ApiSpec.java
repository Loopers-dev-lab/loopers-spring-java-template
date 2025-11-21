package com.loopers.interfaces.api.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Pageable;

import com.loopers.interfaces.api.ApiResponse;

@Tag(name = "Order V1 API", description = "주문 관리 API")
public interface OrderV1ApiSpec {

    /**
     * Register a new order for the specified user.
     *
     * @param username the username placing the order
     * @param request  the order creation details
     * @return         an ApiResponse containing the created order's data
     */
    @Operation(
            summary = "주문 등록",
            description = "새로운 주문을 등록합니다."
    )
    ApiResponse<OrderV1Dtos.OrderCreateResponse> createOrder(
            @Schema(name = "사용자명", description = "주문할 사용자명")
            String username,
            OrderV1Dtos.OrderCreateRequest request
    );

    /**
     * Retrieve a paginated list of orders for the specified user.
     *
     * @param username the username whose orders will be retrieved
     * @param pageable paging information (page number and size)
     * @return an ApiResponse containing a PageResponse of OrderListResponse entries
     */
    @Operation(
            summary = "주문 목록 조회",
            description = "사용자의 주문 목록을 페이징하여 조회합니다."
    )
    ApiResponse<OrderV1Dtos.PageResponse<OrderV1Dtos.OrderListResponse>> getOrders(
            @Schema(name = "사용자명", description = "조회할 사용자명")
            String username,

            @Schema(name = "페이징 정보", description = "페이지 번호와 크기")
            Pageable pageable
    );

    /**
     * Retrieve detailed information for an order by its ID.
     *
     * @param orderId the ID of the order to retrieve
     * @return an ApiResponse containing an OrderV1Dtos.OrderDetailResponse with the order's detailed information
     */
    @Operation(
            summary = "주문 상세 조회",
            description = "주문 ID로 주문 상세 정보를 조회합니다."
    )
    ApiResponse<OrderV1Dtos.OrderDetailResponse> getOrderDetail(
            @Schema(name = "주문 ID", description = "조회할 주문의 ID")
            Long orderId
    );
}