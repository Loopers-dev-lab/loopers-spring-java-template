package com.loopers.infrastructure.platform;

public interface DataPlatformSender {

    void sendOrderResult(OrderResultMessage message);

    void sendPaymentResult(PaymentResultMessage message);

    void sendUserAction(UserActionMessage message);
}
