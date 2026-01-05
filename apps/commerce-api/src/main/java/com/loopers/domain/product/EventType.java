package com.loopers.domain.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    VIEWED("상품 상세 조회"),
    ;

    private final String description;
}
