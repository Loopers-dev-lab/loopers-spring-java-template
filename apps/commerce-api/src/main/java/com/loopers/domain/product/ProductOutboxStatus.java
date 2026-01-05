package com.loopers.domain.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductOutboxStatus {
    PENDING("이벤트 발행 대기 중"),
    PUBLISHED("이벤트 발행 완료"),
    FAILED("이벤트 발행 실패"),
    ;

    private final String description;
}
