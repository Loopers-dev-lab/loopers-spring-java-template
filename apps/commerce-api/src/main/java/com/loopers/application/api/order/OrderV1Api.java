package com.loopers.application.api.order;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderDetail;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.service.order.OrderQueryService;
import com.loopers.core.service.order.OrderService;
import com.loopers.core.service.order.query.GetOrderDetailQuery;
import com.loopers.core.service.order.query.GetOrderListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.loopers.application.api.order.OrderV1Dto.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Api implements OrderV1ApiSpec {

    private final OrderService orderService;
    private final OrderQueryService queryService;

    @Override
    @PostMapping
    public ApiResponse<OrderResponse> order(
            @RequestHeader(name = "X-USER-ID") String userIdentifier,
            @RequestBody OrderRequest request
    ) {
        Order savedOrder = orderService.order(request.toCommand(userIdentifier));

        return ApiResponse.success(new OrderResponse(savedOrder.getId().value()));
    }

    @Override
    @GetMapping
    public ApiResponse<OrderListResponse> getOrderList(
            @RequestHeader(name = "X-USER-ID") String userIdentifier,
            String createdAtSort,
            int pageNo,
            int pageSize
    ) {
        OrderListView view = queryService.getOrderListWithCondition(new GetOrderListQuery(userIdentifier, createdAtSort, pageNo, pageSize));

        return ApiResponse.success(OrderListResponse.from(view));
    }

    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(
            @PathVariable String orderId
    ) {
        OrderDetail orderDetail = queryService.getOrderDetail(new GetOrderDetailQuery(orderId));
        
        return ApiResponse.success(OrderDetailResponse.from(orderDetail));
    }
}
