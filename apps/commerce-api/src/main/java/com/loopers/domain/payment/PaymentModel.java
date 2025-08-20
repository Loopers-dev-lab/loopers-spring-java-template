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
@Table(name = "payment")
@Getter
public class PaymentModel extends BaseEntity {
    
    @Embedded
    private PaymentOrderId orderId;
    
    @Embedded
    private PaymentStatus status; // 결제 상태 (예: initiated, completed, failed)
    
    @Embedded
    private PaymentAmount amount; // 결제 금액
    
    @Embedded
    private PaymentMethod method; // 결제 방법 (예: 카드, 계좌이체 등)
    
    @Embedded
    private PaymentPointUsed pointsUsed; // 사용된 포인트
    
    @Embedded
    private PaymentTransactionId transactionId; // 결제 트랜잭션 ID
    
    @Column(name = "completed_at")
    private ZonedDateTime completedAt; // 결제 완료 시간 (null이면 아직 완료되지 않음)
    
    protected PaymentModel() {}
    
    private PaymentModel(PaymentOrderId orderId, PaymentStatus status, PaymentAmount amount,
                         PaymentMethod method, PaymentPointUsed pointsUsed, PaymentTransactionId transactionId) {
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.method = method;
        this.pointsUsed = pointsUsed;
        this.transactionId = transactionId;
    }

    public static PaymentModel create(Long orderId, BigDecimal amount, String method, BigDecimal pointsUsed) {
        return new PaymentModel(
                PaymentOrderId.of(orderId),
                PaymentStatus.initiated(),
                PaymentAmount.of(amount),
                PaymentMethod.of(method),
                PaymentPointUsed.of(pointsUsed != null ? pointsUsed : BigDecimal.ZERO),
                PaymentTransactionId.empty()
        );
    }

    public void complete(String transactionId) {
        if (this.status.isCompleted()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 완료된 결제입니다.");
        }
        
        this.status = this.status.complete();
        this.transactionId = this.transactionId.update(transactionId);
        this.completedAt = ZonedDateTime.now();
    }
    
    public void fail(String reason) {
        if (this.status.isCompleted()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "완료된 결제는 실패 처리할 수 없습니다.");
        }
        
        this.status = this.status.fail();
    }
    

    public boolean isCompleted() {
        return this.status.isCompleted();
    }
    
    public boolean isPending() {
        return this.status.isPending();
    }
    
    public boolean isFailed() {
        return this.status.isFailed();
    }

    public boolean validateCallback(BigDecimal callbackAmount, String orderNumber) {
        if (callbackAmount == null || orderNumber == null) {
            return false;
        }
        
        boolean amountMatches = this.amount.getValue().compareTo(callbackAmount) == 0;
        

        boolean orderMatches = orderNumber.trim().length() > 0;
        
        return amountMatches && orderMatches;
    }
    
    public Long getOrderIdValue() {
        return this.orderId.getValue();
    }
    
    public BigDecimal getAmountValue() {
        return this.amount.getValue();
    }
    
    public int getAmountIntValue() {
        return this.amount.getIntValue();
    }
    
    public BigDecimal getPointsUsedValue() {
        return this.pointsUsed.getValue();
    }
    
    public int getPointsUsedIntValue() {
        return this.pointsUsed.getIntValue();
    }
    
    public String getMethodValue() {
        return this.method.getValue();
    }
    
    public String getTransactionIdValue() {
        return this.transactionId.getValue();
    }
    
    public String getStatusValue() {
        return this.status.getValue();
    }
}
