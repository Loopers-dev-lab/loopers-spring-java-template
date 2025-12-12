package com.loopers.infrastructure.pg;

import com.loopers.interfaces.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "pgClient", url = "${pg.simulator.base-url}", configuration = PgClientConfig.class)
public interface PgClient {

    @PostMapping("/api/v1/payments")
    ApiResponse<PgV1Dto.PgTransactionResponse> requestPayment(@RequestBody PgV1Dto.PgPaymentRequest request);

    @GetMapping("/api/v1/payments/{transactionKey}")
    ApiResponse<PgV1Dto.PgTransactionDetailResponse> getTransactionDetail(@PathVariable String transactionKey);

    @GetMapping(path = "/api/v1/payments", params = "orderId")
    ApiResponse<PgV1Dto.PgOrderResponse> getPaymentByOrderId(@RequestParam("orderId") String orderId);
}
