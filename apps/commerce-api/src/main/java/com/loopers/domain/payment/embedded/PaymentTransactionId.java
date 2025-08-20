package com.loopers.domain.payment.embedded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.util.UUID;

@Embeddable
@Getter
public class PaymentTransactionId {
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    public PaymentTransactionId() {}
    
    private PaymentTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public static PaymentTransactionId of(String transactionId) {
        if (transactionId != null) {
            validateTransactionId(transactionId);
        }
        return new PaymentTransactionId(transactionId);
    }
    
    public static PaymentTransactionId generate() {
        String uuid = "TXN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return new PaymentTransactionId(uuid);
    }
    
    public static PaymentTransactionId empty() {
        return new PaymentTransactionId(null);
    }
    
    public boolean isEmpty() {
        return this.transactionId == null || this.transactionId.trim().isEmpty();
    }
    
    public boolean isPresent() {
        return !isEmpty();
    }
    
    public PaymentTransactionId update(String transactionId) {
        validateTransactionId(transactionId);
        return new PaymentTransactionId(transactionId);
    }
    
    public String getValue() {
        return transactionId;
    }
    
    private static void validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "트랜잭션 ID는 필수입니다.");
        }
        if (transactionId.length() > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "트랜잭션 ID는 100자를 초과할 수 없습니다.");
        }
    }
}
