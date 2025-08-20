package com.loopers.domain.payment.fixture;

import com.loopers.domain.payment.PaymentCallbackModel;

import java.math.BigDecimal;

public class PaymentCallbackFixture {
    
    public static final Long DEFAULT_PAYMENT_ID = 1L;
    public static final String DEFAULT_ORDER_NUMBER = "ORDER20241201001";
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("50000");
    public static final boolean DEFAULT_SUCCESS = true;
    public static final String DEFAULT_TRANSACTION_ID = "TXN123456789";
    public static final String DEFAULT_SIGNATURE = "abc123signature456def";
    
    public static PaymentCallbackModel createSuccessCallback() {
        return PaymentCallbackModel.create(
                DEFAULT_PAYMENT_ID,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_AMOUNT,
                DEFAULT_SUCCESS,
                DEFAULT_TRANSACTION_ID,
                DEFAULT_SIGNATURE
        );
    }
    
    public static PaymentCallbackModel createFailureCallback() {
        return PaymentCallbackModel.create(
                DEFAULT_PAYMENT_ID,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_AMOUNT,
                false,
                null,
                DEFAULT_SIGNATURE
        );
    }
    
    public static PaymentCallbackModel createCallbackWithPaymentId(Long paymentId) {
        return PaymentCallbackModel.create(
                paymentId,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_AMOUNT,
                DEFAULT_SUCCESS,
                DEFAULT_TRANSACTION_ID,
                DEFAULT_SIGNATURE
        );
    }
    
    public static PaymentCallbackModel createCallbackWithOrderNumber(String orderNumber) {
        return PaymentCallbackModel.create(
                DEFAULT_PAYMENT_ID,
                orderNumber,
                DEFAULT_AMOUNT,
                DEFAULT_SUCCESS,
                DEFAULT_TRANSACTION_ID,
                DEFAULT_SIGNATURE
        );
    }
    
    public static PaymentCallbackModel createCallbackWithAmount(BigDecimal amount) {
        return PaymentCallbackModel.create(
                DEFAULT_PAYMENT_ID,
                DEFAULT_ORDER_NUMBER,
                amount,
                DEFAULT_SUCCESS,
                DEFAULT_TRANSACTION_ID,
                DEFAULT_SIGNATURE
        );
    }
    
    public static PaymentCallbackModel createCallbackWithoutSignature() {
        return PaymentCallbackModel.create(
                DEFAULT_PAYMENT_ID,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_AMOUNT,
                DEFAULT_SUCCESS,
                DEFAULT_TRANSACTION_ID,
                null
        );
    }
    
    public static PaymentCallbackModel createCallbackWithInvalidSignature() {
        return PaymentCallbackModel.create(
                DEFAULT_PAYMENT_ID,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_AMOUNT,
                DEFAULT_SUCCESS,
                DEFAULT_TRANSACTION_ID,
                "invalid"
        );
    }
}
