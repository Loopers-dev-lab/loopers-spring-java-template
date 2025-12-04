package com.loopers.domain.payment;

import com.loopers.interfaces.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "pgClient", url = "${pg.api.url}")
public interface PgFeignClient {

    @PostMapping("/api/v1/payments")
    ApiResponse<PaymentDto.PgResponse> approvePayment(
            @RequestHeader("X-USER-ID") Long userId,
            PaymentDto.PgRequest request
    );
}
