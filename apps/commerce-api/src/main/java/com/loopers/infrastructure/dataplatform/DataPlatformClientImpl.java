package com.loopers.infrastructure.dataplatform;

import com.loopers.domain.dataplatform.DataPlatformClient;
import com.loopers.domain.dataplatform.OrderData;
import com.loopers.domain.dataplatform.PaymentData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataPlatformClientImpl implements DataPlatformClient {

    @Override
    public void sendOrderData(OrderData orderData) {
        log.info("데이터 플랫폼에 주문 데이터 전송: orderKey={}, userId={}, finalAmount={}",
                orderData.orderKey(), orderData.userId(), orderData.finalAmount());

        try {
            log.info("주문 데이터 전송 성공: orderKey={}", orderData.orderKey());
        } catch (Exception e) {
            log.error("주문 데이터 전송 실패: orderKey={}", orderData.orderKey(), e);
        }
    }

    @Override
    public void sendPaymentData(PaymentData paymentData) {
        log.info("데이터 플랫폼에 결제 데이터 전송: orderKey={}, transactionKey={}, status={}, amount={}",
                paymentData.orderKey(), paymentData.transactionKey(), paymentData.paymentStatus(), paymentData.amount());

        try {
            log.info("결제 데이터 전송 성공: orderKey={}", paymentData.orderKey());
        } catch (Exception e) {
            log.error("결제 데이터 전송 실패: orderKey={}", paymentData.orderKey(), e);
        }
    }
}

