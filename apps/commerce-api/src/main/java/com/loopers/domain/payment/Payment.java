package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
public class Payment extends BaseEntity {
    @Column(name = "ref_order_id", nullable = false)
    private Long orderId;

    @Column(name = "ref_user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_key")
    private String transactionKey;

}
