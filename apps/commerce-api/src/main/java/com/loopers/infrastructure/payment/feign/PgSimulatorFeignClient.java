package com.loopers.infrastructure.payment.feign;

import com.loopers.domain.payment.PaymentApproveInfo;
import com.loopers.domain.payment.PaymentApproveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "pg-simulator", url = "${pg.simulator.url:http://localhost:8082}")
public interface PgSimulatorFeignClient {

    @PostMapping("/api/v1/payments")
    PaymentApproveResponse processPayment(@RequestHeader("X-USER-ID") String userId, @RequestBody PaymentApproveInfo request);
}
