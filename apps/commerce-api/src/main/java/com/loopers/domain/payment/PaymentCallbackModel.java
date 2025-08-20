package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.payment.embedded.*;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;


@Entity
@Table(name = "payment_callback")
@Getter
public class PaymentCallbackModel extends BaseEntity {
    
    @Embedded
    private PaymentId paymentId;
    
    @Embedded
    private PaymentOrderNumber orderNumber;
    
    @Embedded
    private PaymentAmount amount;
    
    @Embedded
    private PaymentSuccess success; //
    
    @Embedded
    private PaymentTransactionId transactionId; // 결제 서비스 제공자가 보낸 트랜잭션 ID
    
    @Column(name = "signature", length = 500)
    private String signature; // 결제 서비스 제공자가 보낸 서명
    
    @Column(name = "received_at", nullable = false)
    private ZonedDateTime receivedAt; // 콜백 수신 시간
    
    protected PaymentCallbackModel() {}
    
    private PaymentCallbackModel(PaymentId paymentId, PaymentOrderNumber orderNumber, PaymentAmount amount,
                                 PaymentSuccess success, PaymentTransactionId transactionId,
                                 String signature, ZonedDateTime receivedAt) {
        this.paymentId = paymentId;
        this.orderNumber = orderNumber;
        this.amount = amount;
        this.success = success;
        this.transactionId = transactionId;
        this.signature = signature;
        this.receivedAt = receivedAt;
    }

    public static PaymentCallbackModel create(Long paymentId, String orderNumber, BigDecimal amount,
                                              boolean success, String transactionId, String signature) {
        validateCreateParameters(paymentId, orderNumber, amount);
        
        return new PaymentCallbackModel(
                PaymentId.of(paymentId),
                PaymentOrderNumber.of(orderNumber),
                PaymentAmount.of(amount),
                PaymentSuccess.of(success),
                PaymentTransactionId.of(transactionId),
                signature,
                ZonedDateTime.now()
        );
    }

    public boolean validateSignature() {
        if (this.signature == null || this.signature.trim().isEmpty()) {
            return false;
        }
        return this.signature.length() >= 10; // 임시 검증 로직
    }
    
    public boolean matchesPayment(PaymentModel payment) {
        if (payment == null) {
            return false;
        }
        
        // 결제 ID 일치 확인
        boolean paymentIdMatches = this.paymentId.getValue().equals(payment.getId());
        
        // 금액 일치 확인
        boolean amountMatches = this.amount.getValue().compareTo(payment.getAmountValue()) == 0;
        
        // 트랜잭션 ID 일치 확인 (성공한 경우에만)
        boolean transactionIdMatches = true;
        if (this.success.isSuccess() && payment.getTransactionIdValue() != null) {
            transactionIdMatches = this.transactionId.getValue().equals(payment.getTransactionIdValue());
        }
        
        return paymentIdMatches && amountMatches && transactionIdMatches;
    }
    
    public boolean isSuccess() {
        return this.success.isSuccess();
    }
    
    public boolean isFailure() {
        return this.success.isFailure();
    }
    
    public BigDecimal getAmountValue() {
        return this.amount.getValue();
    }
    
    public int getAmountIntValue() {
        return this.amount.getIntValue();
    }
    
    public String getTransactionIdValue() {
        return this.transactionId.getValue();
    }
    
    public boolean getSuccessValue() {
        return this.success.isSuccess();
    }
    
    public String getOrderNumber() {
        return this.orderNumber.getValue();
    }
    
    public Long getPaymentId() {
        return this.paymentId.getValue();
    }
    
    private static void validateCreateParameters(Long paymentId, String orderNumber, BigDecimal amount) {
        if (paymentId == null || paymentId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 ID는 필수입니다.");
        }
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 번호는 필수입니다.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0 이상이어야 합니다.");
        }
    }
}
