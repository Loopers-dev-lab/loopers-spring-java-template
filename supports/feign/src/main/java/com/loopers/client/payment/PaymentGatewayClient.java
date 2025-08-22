package com.loopers.client.payment;

import com.loopers.client.payment.dto.ApiResponse;
import com.loopers.client.payment.dto.PaymentV1Dto;
import com.loopers.config.feign.FeignConfig;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    value = "loopers-payment",
        url = "${pg.loopers}",
        configuration = FeignConfig.class)
public interface PaymentGatewayClient {

    /**
     * 결제 요청 API
     */
    @PostMapping(consumes = "application/json")
    ApiResponse<PaymentV1Dto.Response> requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @Valid @RequestBody PaymentV1Dto.Request request
    );

    /**
     * 결제 정보 조회 API
     */
    @GetMapping("{transactionKey}")
    ApiResponse<PaymentV1Dto.Response.Detail> getPaymentDetail(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable("transactionKey") String transactionKey
    );

    /**
     *  정보 조회 API,주문별 결제 내역 조회 API
     */
    @GetMapping
    ApiResponse<Object> getPaymentsByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam("orderId") String orderId
    );
}
