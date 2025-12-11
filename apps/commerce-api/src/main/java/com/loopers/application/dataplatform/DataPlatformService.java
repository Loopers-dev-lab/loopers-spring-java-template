package com.loopers.application.dataplatform;

import com.loopers.application.event.OrderDataTransferEvent;
import com.loopers.application.event.PaymentDataTransferEvent;

public interface DataPlatformService {
    void sendOrderData(OrderDataTransferEvent event);
    void sendPaymentData(PaymentDataTransferEvent event);
}
