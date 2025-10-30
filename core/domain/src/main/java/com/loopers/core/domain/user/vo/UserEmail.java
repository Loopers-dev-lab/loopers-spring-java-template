package com.loopers.core.domain.user.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

public record UserEmail(String value) {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public UserEmail(String value) {
        validateNotNull(value);
        this.value = value;
    }

    public static UserEmail create(String email) {
        validateNotNull(email);
        validate(email);

        return new UserEmail(email);
    }

    private static String validateNotNull(String email) {
        return Objects.requireNonNull(email, DomainErrorCode.notNullMessage("사용자의 이메일"));
    }

    private static void validate(String email) {
        if (!email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException(DomainErrorCode.INVALID_EMAIL_FORMAT.getMessage());
        }
    }
}
