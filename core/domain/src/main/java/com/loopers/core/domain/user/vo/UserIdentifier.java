package com.loopers.core.domain.user.vo;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Objects;

import static com.loopers.core.domain.error.DomainErrorCode.USER_ID_LENGTH_VALIDATE;
import static com.loopers.core.domain.error.DomainErrorCode.USER_ID_MUST_BE_ENG_OR_NUMBER;

public record UserIdentifier(String value) {

    private static final String IDENTIFIER_REGEX = "^[a-zA-Z0-9]+$";
    private static final int IDENTIFIER_MAX_LENGTH = 10;
    private static final int IDENTIFIER_MIN_LENGTH = 1;

    public UserIdentifier(String value) {
        validateNotNull(value);

        this.value = value;
    }

    public static UserIdentifier create(String value) {
        validateNotNull(value);
        validate(value);

        return new UserIdentifier(value);
    }

    private static void validateNotNull(String value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("사용자의 ID"));
    }

    private static void validate(String identifier) {
        if (!identifier.matches(IDENTIFIER_REGEX)) {
            throw new IllegalArgumentException(USER_ID_MUST_BE_ENG_OR_NUMBER.getMessage());
        }

        if (identifier.length() < IDENTIFIER_MIN_LENGTH || identifier.length() > IDENTIFIER_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format(USER_ID_LENGTH_VALIDATE.getMessage(), IDENTIFIER_MIN_LENGTH, IDENTIFIER_MAX_LENGTH)
            );
        }
    }
}
