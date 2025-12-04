package com.loopers.infrastructure.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "pg-client",
    url = "${pg.base-url}",
    configuration = PgClientConfig.class
)
public interface PgClient {

    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody PgPaymentRequest request
    );

    @GetMapping("/api/v1/payments/{transactionKey}")
    PgTransactionDetail getPaymentDetail(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable("transactionKey") String transactionKey
    );

    @GetMapping("/api/v1/payments")
    PgTransactionDetail getPaymentByOrderId(
        @RequestHeader("X-USER-ID") String userId,
        @RequestParam("orderId") String orderId
    );
}
