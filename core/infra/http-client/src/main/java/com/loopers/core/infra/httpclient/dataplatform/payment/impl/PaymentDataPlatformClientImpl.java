package com.loopers.core.infra.httpclient.dataplatform.payment.impl;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PaymentDataPlatformClient;
import org.springframework.stereotype.Component;

@Component
public class PaymentDataPlatformClientImpl implements PaymentDataPlatformClient {

    @Override
    public void send(Payment payment) {

    }
}
