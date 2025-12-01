package com.loopers.core.domain.payment;

public interface PgClient {

    PgPayment pay(Payment payment, String callbackUrl);
}
