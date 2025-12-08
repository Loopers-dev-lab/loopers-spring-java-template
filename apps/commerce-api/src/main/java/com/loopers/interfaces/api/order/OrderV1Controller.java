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
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;
    private final UserFacade userFacade;


    /**
     * 포인트 결제 주문 생성
     * <p>
     * 포인트로 즉시 결제하고 주문을 확정합니다.
     */
    @PostMapping(Uris.Order.CREATE_POINT)
    public ApiResponse<OrderV1Dtos.OrderCreateResponse> createOrderWithPoint(
            @RequestHeader("X-USER-ID") String username,
            @RequestBody OrderV1Dtos.PointOrderCreateRequest request
    ) {
        OrderCreateCommand command = request.toCommand(username);
        OrderInfo orderInfo = orderFacade.createOrderByPoint(command);
        OrderV1Dtos.OrderCreateResponse response = OrderV1Dtos.OrderCreateResponse.from(orderInfo);
        return ApiResponse.success(response);
    }

    /**
     * 카드 결제 주문 생성
     * <p>
     * 주문 생성과 동시에 PG 결제를 요청합니다.
     * 결제는 비동기로 처리되며, 콜백을 통해 최종 결과를 받습니다.
     */
    @PostMapping(Uris.Order.CREATE_CARD)
    public ApiResponse<OrderV1Dtos.OrderCreateResponse> createOrderWithCard(
            @RequestHeader("X-USER-ID") String username,
            @RequestBody OrderV1Dtos.CardOrderCreateRequest request
    ) {
        OrderCreateCommand command = request.toCommand(username);
        OrderFacade.OrderWithPaymentInfo result = orderFacade.createOrderWithCardPayment(command);
        OrderV1Dtos.OrderCreateResponse response = OrderV1Dtos.OrderCreateResponse.from(
                result.order(),
                result.payment()
        );
        return ApiResponse.success(response);
    }

    @GetMapping(Uris.Order.GET_LIST)
    @Override
    public ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>> getOrders(
            @RequestHeader("X-USER-ID") String username,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UserInfo userInfo = userFacade.getUserByUsername(username);
        Page<OrderSummary> orders = orderFacade.getOrderSummariesByUserId(userInfo.id(), pageable);
        Page<OrderV1Dtos.OrderListResponse> responsePage = orders.map(summary ->
                new OrderV1Dtos.OrderListResponse(
                        summary.id(),
                        summary.orderNumber(),
                        summary.status(),
                        summary.originalTotalAmount(),
                        summary.discountAmount(),
                        summary.finalTotalAmount(),
                        summary.createdAt()
                )
        );
        return ApiResponse.success(PageResponse.from(responsePage));
    }

    @GetMapping(Uris.Order.GET_DETAIL)
    @Override
    public ApiResponse<OrderV1Dtos.OrderDetailResponse> getOrderDetail(
            @RequestHeader("X-USER-ID") String username,
            @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = orderFacade.getOrderById(username, orderId);
        OrderV1Dtos.OrderDetailResponse response = OrderV1Dtos.OrderDetailResponse.from(orderInfo);
        return ApiResponse.success(response);
    }
}
