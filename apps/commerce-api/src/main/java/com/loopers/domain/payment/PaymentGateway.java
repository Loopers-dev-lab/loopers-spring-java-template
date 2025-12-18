package com.loopers.domain.payment;

public interface PaymentGateway {
    PaymentApproveResponse approvePayment(String userId, PaymentApproveInfo request);
}
