package com.loopers.infrastructure.external.feign;

import com.loopers.config.FeignClientConfig;
import com.loopers.infrastructure.external.dto.PaymentExternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PG 결제 시스템 FeignClient
 * X-USER-ID 헤더는 FeignClientConfig의 pgClientIdInterceptor에서 자동으로 추가됨
 */
@FeignClient(
        name = "pg-simulator",
        url = "${external.pg-simulator.url}",
        configuration = FeignClientConfig.class
    )
public interface PaymentClient {

    @PostMapping("/api/v1/payments")
    PaymentExternalDto.PaymentResponse payment(
            @RequestBody PaymentExternalDto.PaymentRequest request
    );

    /**
     * 결제 정보 확인
     * */
    @GetMapping("/api/v1/payments/{transactionId}")
    PaymentExternalDto.PaymentResponse paymentInfo(
            @PathVariable String transactionId
    );

    /**
     * 주문에 엮인 결제 정보 조회
     * */
    @GetMapping("/api/v1/payments")
    List<PaymentExternalDto.PaymentResponse> paymentList(
            @RequestParam("orderId") String orderId
    );

}
