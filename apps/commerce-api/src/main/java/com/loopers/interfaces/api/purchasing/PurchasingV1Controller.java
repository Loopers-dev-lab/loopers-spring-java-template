package com.loopers.interfaces.api.purchasing;

import com.loopers.application.purchasing.OrderInfo;
import com.loopers.application.purchasing.PurchasingFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주문 API v1 컨트롤러.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class PurchasingV1Controller {

    private final PurchasingFacade purchasingFacade;

    /**
     * Create a new order for the specified user.
     *
     * @param userId the user identifier provided in the `X-USER-ID` header
     * @param request the order creation request payload
     * @return the created order represented as a `PurchasingV1Dto.OrderResponse`
     */
    @PostMapping
    public ApiResponse<PurchasingV1Dto.OrderResponse> createOrder(
        @RequestHeader("X-USER-ID") String userId,
        @Valid @RequestBody PurchasingV1Dto.CreateRequest request
    ) {
        OrderInfo orderInfo = purchasingFacade.createOrder(userId, request.toCommands());
        return ApiResponse.success(PurchasingV1Dto.OrderResponse.from(orderInfo));
    }

    /**
     * Retrieves the current user's orders.
     *
     * @param userId the user identifier taken from the X-USER-ID request header
     * @return an OrdersResponse containing the list of the user's orders
     */
    @GetMapping
    public ApiResponse<PurchasingV1Dto.OrdersResponse> getOrders(
        @RequestHeader("X-USER-ID") String userId
    ) {
        List<OrderInfo> orderInfos = purchasingFacade.getOrders(userId);
        return ApiResponse.success(PurchasingV1Dto.OrdersResponse.from(orderInfos));
    }

    /**
     * Retrieves the specified order for the current user.
     *
     * @param userId  value of the `X-USER-ID` header identifying the current user
     * @param orderId identifier of the order to retrieve
     * @return an OrderResponse containing detailed information for the specified order
     */
    @GetMapping("/{orderId}")
    public ApiResponse<PurchasingV1Dto.OrderResponse> getOrder(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = purchasingFacade.getOrder(userId, orderId);
        return ApiResponse.success(PurchasingV1Dto.OrderResponse.from(orderInfo));
    }
}

