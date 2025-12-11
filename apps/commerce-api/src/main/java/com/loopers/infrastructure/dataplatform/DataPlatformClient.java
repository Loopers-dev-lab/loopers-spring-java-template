package com.loopers.infrastructure.dataplatform;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 데이터 플랫폼 연동 클라이언트
 * - 주문/결제 데이터를 외부 데이터 플랫폼으로 전송
 * - 비동기 이벤트 기반으로 동작하여 핵심 비즈니스 로직과 분리
 */
@FeignClient(
    name = "data-platform-client",
    url = "${data-platform.base-url:http://localhost:9999}",
    configuration = DataPlatformClientConfig.class
)
public interface DataPlatformClient {

    /**
     * 주문 데이터 전송
     */
    @PostMapping("/api/v1/orders")
    DataPlatformResponse sendOrderData(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody DataPlatformOrderRequest request
    );

    /**
     * 결제 데이터 전송
     */
    @PostMapping("/api/v1/payments")
    DataPlatformResponse sendPaymentData(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody DataPlatformPaymentRequest request
    );
}
