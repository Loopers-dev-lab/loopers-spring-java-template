package com.loopers.infrastructure;

import com.loopers.interfaces.api.payment.PaymentV1Dto.PaymentHistoryResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dto.PaymentResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dto.PgPaymentRequest;
import com.loopers.interfaces.api.payment.PaymentV1Dto.PgResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "pgClient",
        url = "http://localhost:8082",
        configuration = FeignClientTimeoutConfig.class
)
public interface PgClient {
    @PostMapping("/api/v1/payments")
    PaymentResponse requestPayment(@RequestBody PgPaymentRequest request);

    @GetMapping("/api/v1/payments/{transactionKey}")
    PgResponse<PaymentResponse> getPaymentByTransactionKey(@PathVariable("transactionKey") String transactionKey);

    @GetMapping("/api/v1/payments")
    PgResponse<PaymentHistoryResponse> getPaymentByOrderId(@RequestParam("orderId") Long orderId);
}
