package com.loopers.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "commerce_payment_history")
public class CommercePaymentHistory extends BaseEntity {

    // 1. 이 이력이 발생한 시점의 상태 (승인성공, 승인실패, 취소완료 등)
    @Enumerated(EnumType.STRING)
    private PaymentDto.PaymentStatus status;

    // 2. 이 이력에서 변동된 금액 (부분 취소 등을 고려하여 필수)
    // 예: 결제시 30000, 부분취소시 -10000
    private Long amount;

    // 3. PG사 거래 고유 키 (tid/transactionKey)
    // 취소 요청하거나, PG사 어드민에서 건별 조회할 때 무조건 필요
    private String transactionKey;

    // 4. 사유 (실패 원인 또는 취소 사유)
    // 예: "잔액 부족", "고객 변심 반품"
    private String reason;

    // 5. PG사 응답 원본 (JSON String)
    // ★ 실무 필수: 나중에 "PG사는 줬다는데 우린 왜 없어?" 할 때 증거 자료
    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawData;

    @Builder
    public CommercePaymentHistory(
            PaymentDto.PaymentStatus status,
            Long amount,
            String transactionKey,
            String reason,
            String rawData
    ) {
        this.status = status;
        this.amount = amount;
        this.transactionKey = transactionKey;
        this.reason = reason;
        this.rawData = rawData;
    }

    @Override
    protected void guard() {
        if(status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "CommercePaymentHistory : status가 비어있을 수 없습니다.");
        }
        if(amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "CommercePaymentHistory : amount가 비어있을 수 없습니다.");
        }
        if(transactionKey == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "CommercePaymentHistory : transactionKey가 비어있을 수 없습니다.");
        }
    }

}

