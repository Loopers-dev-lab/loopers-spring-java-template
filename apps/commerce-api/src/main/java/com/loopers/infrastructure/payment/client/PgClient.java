package com.loopers.infrastructure.payment.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.loopers.infrastructure.payment.client.dto.PgPaymentRequest;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

/**
 * PG Simulator Feign Client
 */
@FeignClient(
        name = "pgClient",
        url = "${pg.simulator.url}",
        configuration = PgClientConfig.class
)
public interface PgClient {

    /**
     * 결제 요청
     */
    @PostMapping("/api/v1/payments")
    PgPaymentResponse requestPayment(
            @RequestHeader("X-USER-ID") String userName,
            @RequestBody PgPaymentRequest request
    );

    /**
     * 결제 정보 확인 (by transactionKey)
     */
    @GetMapping("/api/v1/payments/{transactionKey}")
    PgPaymentResponse getPayment(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable String transactionKey
    );

    /**
     * 결제 정보 목록 조회 (by orderNumber)
     */
    @GetMapping("/api/v1/payments")
    List<PgPaymentResponse> getPaymentsByOrderId(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String orderId
    );
}
