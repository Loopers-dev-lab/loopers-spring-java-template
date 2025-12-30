package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPaymentProcessor;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;
    private final OrderPaymentProcessor orderPaymentProcessor;

    @Override
    @PostMapping
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
            @RequestHeader("X-USER-ID") String userId,
            @Valid @RequestBody OrderV1Dto.OrderCreateRequest request
    ) {
        OrderInfo orderInfo = orderFacade.createOrder(request.toCommand(userId));
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }

    @Override
    @PostMapping("/{orderReference}/callback")
    public ApiResponse<Object> callback(
            @PathVariable("orderReference") String orderReference,
            @Valid @RequestBody OrderV1Dto.PaymentCallbackRequest request
    ) {
        orderPaymentProcessor.handlePaymentCallback(orderReference, request.status(), request.transactionKey(), request.reason());
        return ApiResponse.success();
    }

    @Override
    @PostMapping("/{orderId}/sync")
    public ApiResponse<Object> syncPayment(@PathVariable("orderId") Long orderId) {
        orderPaymentProcessor.syncPayment(orderId);
        return ApiResponse.success();
    }
}
