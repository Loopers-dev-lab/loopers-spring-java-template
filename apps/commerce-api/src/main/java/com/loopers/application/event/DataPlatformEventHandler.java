package com.loopers.application.event;

import com.loopers.application.dataplatform.DataPlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataPlatformEventHandler {
    private final DataPlatformService dataPlatformService;

    @EventListener
    @Async
    public void handleOrderDataTransfer(OrderDataTransferEvent event) {
        log.debug("주문 데이터 플랫폼 전송 이벤트 처리 - 주문ID: {}, 이벤트타입: {}", 
            event.orderId(), event.eventType());
        
        try {
            dataPlatformService.sendOrderData(event);
        } catch (Exception e) {
            log.error("주문 데이터 플랫폼 전송 실패 - 주문ID: {}", event.orderId(), e);
            // TODO: 실패 시 재시도 로직 또는 DLQ 처리
        }
    }

    @EventListener
    @Async
    public void handlePaymentDataTransfer(PaymentDataTransferEvent event) {
        log.debug("결제 데이터 플랫폼 전송 이벤트 처리 - 주문ID: {}, 이벤트타입: {}", 
            event.orderId(), event.eventType());
        
        try {
            dataPlatformService.sendPaymentData(event);
        } catch (Exception e) {
            log.error("결제 데이터 플랫폼 전송 실패 - 주문ID: {}", event.orderId(), e);
            // TODO: 실패 시 재시도 로직 또는 DLQ 처리
        }
    }
}
