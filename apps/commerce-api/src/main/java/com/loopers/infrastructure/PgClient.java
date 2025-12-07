package com.loopers.infrastructure;

import com.loopers.interfaces.api.payment.PaymentV1Dto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "pgClient",
        url = "http://localhost:8082",
        configuration = FeignClientTimeoutConfig.class
)
public interface PgClient {
    @PostMapping("/api/v1/payments")
    PaymentV1Dto.PgPaymentResponse requestPayment(@RequestBody PaymentV1Dto.PgPaymentRequest request);

    @GetMapping("/api/v1/payments/{transactionKey}")
    PaymentV1Dto.PgPaymentResponse getPaymentByTransactionKey(@PathVariable("transactionKey") String transactionKey);

    @GetMapping("/api/v1/payments")
    PaymentV1Dto.PgPaymentResponse getPaymentByOrderId(@RequestParam("orderId") Long orderId);
}
