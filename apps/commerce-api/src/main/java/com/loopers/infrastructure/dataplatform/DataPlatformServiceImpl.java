package com.loopers.infrastructure.dataplatform;

import com.loopers.application.dataplatform.DataPlatformService;
import com.loopers.application.event.OrderDataTransferEvent;
import com.loopers.application.event.PaymentDataTransferEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataPlatformServiceImpl implements DataPlatformService {

  @Override
  public void sendOrderData(OrderDataTransferEvent event) {
    log.info("데이터 플랫폼으로 주문 데이터 전송 - 주문ID: {}, 이벤트타입: {}, 상태: {}",
        event.orderId(), event.eventType(), event.status());

    try {
      Thread.sleep(100);
      log.info("주문 데이터 전송 완료 - 주문ID: {}", event.orderId());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("주문 데이터 전송 중 오류 발생 - 주문ID: {}", event.orderId(), e);
    }
  }

  @Override
  public void sendPaymentData(PaymentDataTransferEvent event) {
    log.info("데이터 플랫폼으로 결제 데이터 전송 - 주문ID: {}, 이벤트타입: {}, 상태: {}",
        event.orderId(), event.eventType(), event.transactionStatus());

    try {
      Thread.sleep(100);
      log.info("결제 데이터 전송 완료 - 주문ID: {}", event.orderId());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("결제 데이터 전송 중 오류 발생 - 주문ID: {}", event.orderId(), e);
    }
  }
}
