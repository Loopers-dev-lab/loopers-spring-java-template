package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderRequest;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {
    private final OrderFacade orderFacade;

    @RequestMapping(method = RequestMethod.POST)
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestBody OrderRequest request
    ) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        request.validate();
        OrderInfo orderInfo = orderFacade.createOrder(userId, request);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);
        return ApiResponse.success(response);
    }

    @RequestMapping(method = RequestMethod.GET)
    @Override
    public ApiResponse<OrderV1Dto.OrderPageResponse> getOrderList(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        Page<OrderInfo> orderInfos = orderFacade.getOrderList(userId, pageable);
        OrderV1Dto.OrderPageResponse response = OrderV1Dto.OrderPageResponse.from(orderInfos);
        return ApiResponse.success(response);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{orderId}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrderDetail(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long orderId) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        OrderInfo orderInfo = orderFacade.getOrderInfo(userId, orderId);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);
        return ApiResponse.success(response);
    }
}
