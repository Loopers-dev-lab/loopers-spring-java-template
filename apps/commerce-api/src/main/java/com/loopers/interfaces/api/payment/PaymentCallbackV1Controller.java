package com.loopers.interfaces.api.payment;

import com.loopers.application.order.OrderFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments/callback")
@Slf4j
public class PaymentCallbackV1Controller {
    private final OrderFacade orderFacade;

    @PostMapping
    public ApiResponse<Void> handleCallback(
            @RequestParam("orderId") String orderId,  // URL 파라미터로 orderId 받음
            @RequestBody PaymentV1Dto.PaymentCallbackRequest request) {
        log.info("PG 콜백 수신 - orderId: {}, request: {}", orderId, request);
        orderFacade.handlePgPaymentCallback(orderId, request);

        return ApiResponse.success(null);
    }
}
