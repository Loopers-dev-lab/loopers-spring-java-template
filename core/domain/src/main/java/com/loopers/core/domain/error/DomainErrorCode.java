package com.loopers.core.domain.error;

import lombok.Getter;

@Getter
public enum DomainErrorCode {

    MUST_BE_NOT_NULL(1000, "%s는(은) Null이 될 수 없습니다."),
    USER_ID_MUST_BE_ENG_OR_NUMBER(1001, "사용자의 ID는 영문 또는 숫자로만 이루어져야합니다."),
    USER_ID_LENGTH_VALIDATE(1002, "사용자의 ID의 길이는 %d자 이상 %d자 이하여야 합니다."),
    INVALID_EMAIL_FORMAT(1003, "유효하지 않은 이메일 형식입니다.");

    private final int code;

    private final String message;

    DomainErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String notNullMessage(String fieldName) {
        return String.format(MUST_BE_NOT_NULL.message, fieldName);
    }
}
