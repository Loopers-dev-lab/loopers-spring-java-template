package com.loopers.infrastructure.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentCallbackUrlGenerator {

    @Value("${commerce.api.base-url}")
    private String apiBaseUrl;

    @Value("${commerce.api.callback.base-path}")
    private String callbackBasePath;

    // 주문 ID를 기반으로 콜백 URL 생성
    // 형식: http://localhost:8080/api/v1/payments/callback?orderId={orderId}
    public String generateCallbackUrl(String orderId) {
        return String.format("%s%s?orderId=%s", apiBaseUrl, callbackBasePath, orderId);
    }
}
