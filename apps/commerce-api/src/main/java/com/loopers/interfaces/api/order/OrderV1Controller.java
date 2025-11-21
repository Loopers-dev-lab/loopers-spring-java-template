package com.loopers.interfaces.api.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderSummary;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;
    private final UserFacade userFacade;

    /**
     * Creates a new order for the user identified by the "X-USER-ID" request header.
     *
     * @param username the username extracted from the "X-USER-ID" request header
     * @param request  the order creation payload
     * @return an ApiResponse containing the created order details as an OrderCreateResponse
     */
    @PostMapping(Uris.Order.CREATE)
    @Override
    public ApiResponse<OrderV1Dtos.OrderCreateResponse> createOrder(
            @RequestHeader("X-USER-ID") String username,
            @RequestBody OrderV1Dtos.OrderCreateRequest request
    ) {
        OrderCreateCommand command = request.toCommand(username);
        OrderInfo orderInfo = orderFacade.createOrder(command);
        OrderV1Dtos.OrderCreateResponse response = OrderV1Dtos.OrderCreateResponse.from(orderInfo);
        return ApiResponse.success(response);
    }

    /**
     * Returns a paginated list of the user's order summaries.
     *
     * @param username the user identifier extracted from the "X-USER-ID" request header
     * @param pageable pagination and sorting parameters; default page size is 20
     * @return an ApiResponse wrapping a PageResponse of OrderV1Dtos.OrderListResponse representing the user's orders
     */
    @GetMapping(Uris.Order.GET_LIST)
    @Override
    public ApiResponse<OrderV1Dtos.PageResponse<OrderV1Dtos.OrderListResponse>> getOrders(
            @RequestHeader("X-USER-ID") String username,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UserInfo userInfo = userFacade.getUserByUsername(username);
        Page<OrderSummary> orders = orderFacade.getOrderSummariesByUserId(userInfo.id(), pageable);
        Page<OrderV1Dtos.OrderListResponse> responsePage = orders.map(summary ->
                new OrderV1Dtos.OrderListResponse(
                        summary.id(),
                        summary.status(),
                        summary.totalAmount(),
                        summary.createdAt()
                )
        );
        return ApiResponse.success(OrderV1Dtos.PageResponse.from(responsePage));
    }

    /**
     * Retrieve detailed information for a specific order.
     *
     * @param orderId the identifier of the order to retrieve
     * @return an ApiResponse containing the order detail DTO
     */
    @GetMapping(Uris.Order.GET_DETAIL)
    @Override
    public ApiResponse<OrderV1Dtos.OrderDetailResponse> getOrderDetail(
            @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = orderFacade.getOrderById(orderId);
        OrderV1Dtos.OrderDetailResponse response = OrderV1Dtos.OrderDetailResponse.from(orderInfo);
        return ApiResponse.success(response);
    }
}