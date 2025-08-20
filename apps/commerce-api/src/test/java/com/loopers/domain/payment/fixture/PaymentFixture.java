package com.loopers.domain.payment.fixture;

import com.loopers.domain.payment.PaymentModel;

import java.math.BigDecimal;

/**
 * Payment 테스트용 Fixture 클래스
 */
public class PaymentFixture {
    
    public static final Long DEFAULT_ORDER_ID = 1L;
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("50000");
    public static final String DEFAULT_METHOD = "CARD";
    public static final BigDecimal DEFAULT_POINTS_USED = BigDecimal.ZERO;
    public static final String DEFAULT_TRANSACTION_ID = "TXN123456789";
    
    /**
     * 기본값으로 Payment를 생성한다
     */
    public static PaymentModel createPayment() {
        return PaymentModel.create(
                DEFAULT_ORDER_ID,
                DEFAULT_AMOUNT,
                DEFAULT_METHOD,
                DEFAULT_POINTS_USED
        );
    }
    
    /**
     * 특정 주문 ID로 Payment를 생성한다
     */
    public static PaymentModel createPaymentWithOrderId(Long orderId) {
        return PaymentModel.create(
                orderId,
                DEFAULT_AMOUNT,
                DEFAULT_METHOD,
                DEFAULT_POINTS_USED
        );
    }
    
    /**
     * 특정 금액으로 Payment를 생성한다
     */
    public static PaymentModel createPaymentWithAmount(BigDecimal amount) {
        return PaymentModel.create(
                DEFAULT_ORDER_ID,
                amount,
                DEFAULT_METHOD,
                DEFAULT_POINTS_USED
        );
    }
    
    /**
     * 특정 결제 수단으로 Payment를 생성한다
     */
    public static PaymentModel createPaymentWithMethod(String method) {
        return PaymentModel.create(
                DEFAULT_ORDER_ID,
                DEFAULT_AMOUNT,
                method,
                DEFAULT_POINTS_USED
        );
    }
    
    /**
     * 특정 포인트 사용량으로 Payment를 생성한다
     */
    public static PaymentModel createPaymentWithPointsUsed(BigDecimal pointsUsed) {
        return PaymentModel.create(
                DEFAULT_ORDER_ID,
                DEFAULT_AMOUNT,
                DEFAULT_METHOD,
                pointsUsed
        );
    }
    
    /**
     * 완료된 Payment를 생성한다
     */
    public static PaymentModel createCompletedPayment() {
        PaymentModel payment = createPayment();
        payment.complete(DEFAULT_TRANSACTION_ID);
        return payment;
    }
    
    /**
     * 실패한 Payment를 생성한다
     */
    public static PaymentModel createFailedPayment() {
        PaymentModel payment = createPayment();
        payment.fail("결제 실패");
        return payment;
    }
    
    /**
     * 복합 결제 Payment를 생성한다 (카드 + 포인트)
     */
    public static PaymentModel createMixedPayment() {
        return PaymentModel.create(
                DEFAULT_ORDER_ID,
                DEFAULT_AMOUNT,
                "MIXED",
                new BigDecimal("10000")
        );
    }
    
    /**
     * 포인트 전용 결제 Payment를 생성한다
     */
    public static PaymentModel createPointOnlyPayment() {
        return PaymentModel.create(
                DEFAULT_ORDER_ID,
                new BigDecimal("5000"),
                "POINT",
                new BigDecimal("5000")
        );
    }
}
