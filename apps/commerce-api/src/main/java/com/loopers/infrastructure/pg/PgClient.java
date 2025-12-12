package com.loopers.infrastructure.pg;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "pgClient",
        url = "${pg.url}"
)
public interface PgClient {

    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody PgPaymentRequest request
    );

    @GetMapping("/api/v1/payments/{transactionId}")
    PgPaymentResponse getPaymentDetail(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable("transactionId") String transactionId
    );

    @GetMapping("/api/v1/payments")
    List<PgPaymentResponse> getPaymentsByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam("orderId") String orderId
    );
}
