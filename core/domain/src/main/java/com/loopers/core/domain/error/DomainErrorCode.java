package com.loopers.core.domain.error;

import lombok.Getter;

@Getter
public enum DomainErrorCode {

    MUST_BE_NOT_NULL(1000, "%s는(은) Null이 될 수 없습니다."),
    USER_ID_MUST_BE_ENG_OR_NUMBER(1001, "사용자의 ID는 영문 또는 숫자로만 이루어져야합니다."),
    USER_ID_LENGTH_VALIDATE(1002, "사용자의 ID의 길이는 %d자 이상 %d자 이하여야 합니다."),
    INVALID_EMAIL_FORMAT(1003, "유효하지 않은 이메일 형식입니다."),
    INVALID_BIRTHDATE_FORMAT(1004, "생년월일은 yyyy-MM-dd 형식이어야 합니다."),
    GENDER_IS_MALE_OR_FEMALE(1004, "성별은 MALE 혹은 FEMALE이어야 합니다."),
    PRESENT_USER_IDENTIFIER(1005, "이미 존재하는 사용자 ID입니다."),
    NOT_FOUND(1006, "%s를 찾지 못했습니다."),
    USER_POINT_BALANCE_GRATER_THAN_ZERO(1007, "사용자 포인트의 잔액은 0보다는 커야합니다."),
    CANNOT_CHARGE_POINTS_LESS_THAN_ZERO(1008, "0보다 작거나 같은 포인트를 충전할 수 없습니다.");

    private final int code;

    private final String message;

    DomainErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String notNullMessage(String fieldName) {
        return String.format(MUST_BE_NOT_NULL.message, fieldName);
    }

    public static String notFoundMessage(String fieldName) {
        return String.format(NOT_FOUND.message, fieldName);
    }
}
