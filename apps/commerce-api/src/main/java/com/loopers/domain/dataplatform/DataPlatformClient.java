package com.loopers.domain.dataplatform;

public interface DataPlatformClient {

    void sendOrderData(OrderData orderData);

    void sendPaymentData(PaymentData paymentData);

}

