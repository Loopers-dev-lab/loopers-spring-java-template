package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
public class Payment extends BaseEntity {
    @Column(name = "ref_user_login_id", nullable = false)
    private String userId;

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(name = "transaction_key", nullable = false, unique = true)
    private String transactionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;


    public Payment(String userId, String orderNo, String transactionKey, PaymentStatus status) {
        this.userId = userId;
        this.orderNo = orderNo;
        this.transactionKey = transactionKey;
        this.status = status;
    }

    public static Payment create(String userId, String orderNo, String transactionKey, PaymentStatus status) {
        return new Payment(
                userId,
                orderNo,
                transactionKey,
                PaymentStatus.PENDING
        );
    }
}
