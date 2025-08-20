package com.loopers.domain.payment.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PaymentOrderNumber {
    @Column(name = "order_number", nullable = false, length = 100)
    private String orderNumber;

    public PaymentOrderNumber() {}
    private PaymentOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    public static PaymentOrderNumber of(String orderNumber) {
        if (orderNumber == null || orderNumber.isEmpty()) {
            throw new IllegalArgumentException("주문 번호는 필수입니다.");
        }
        return new PaymentOrderNumber(orderNumber);
    }
    public String getValue() {
        return this.orderNumber;
    }


}
