package com.loopers.infrastructure.dataplatform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataPlatform {

    /**
     * 주문 데이터를 외부 플랫폼으로 전송
     *
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     * @param totalAmount 총 주문 금액
     * @return 전송 성공 여부
     */
    public boolean sendOrderData(Long orderId, Long userId, String totalAmount) {
        log.info("데이터 플랫폼으로 주문 데이터 전송 - orderId: {}, userId: {}, totalAmount: {}",
                orderId, userId, totalAmount);

        return true;
    }

    /**
     * 결제 완료 데이터를 외부 플랫폼으로 전송
     *
     * @param orderId 주문 ID
     * @param paymentId 결제 ID
     * @param paymentType 결제 수단
     * @return 전송 성공 여부
     */
    public boolean sendPaymentData(Long orderId, String paymentId, String paymentType) {
        log.info("데이터 플랫폼으로 결제 데이터 전송 - orderId: {}, paymentId: {}, paymentType: {}",
                orderId, paymentId, paymentType);

        return true;
    }
}
