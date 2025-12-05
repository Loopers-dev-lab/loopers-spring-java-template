package com.loopers.domain.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("대기중"),
    PAID("결제 완료"),
    FAILED("실패"),
    ;
    private final String text;
}
