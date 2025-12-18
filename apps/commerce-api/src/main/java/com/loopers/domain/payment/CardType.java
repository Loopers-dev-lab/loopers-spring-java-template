package com.loopers.domain.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CardType {
    SAMSUNG("삼성"),
    KB("국민은행"),
    HYUNDAI("현대카드"),
    ;
    private final String text;
}
