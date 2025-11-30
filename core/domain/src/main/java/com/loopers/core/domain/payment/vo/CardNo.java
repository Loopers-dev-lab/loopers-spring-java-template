package com.loopers.core.domain.payment.vo;

import java.util.Objects;

import static com.loopers.core.domain.error.DomainErrorCode.notNullMessage;

public record CardNo(String value) {

    private static final String FIELD_NAME = "카드 번호";

    public CardNo {
        validateNull(value);
    }

    private static void validateNull(String value) {
        if (Objects.isNull(value)) {
            throw new NullPointerException(notNullMessage(FIELD_NAME));
        }
    }
}
