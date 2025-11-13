package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessages;
import com.loopers.support.error.ErrorType;

public enum Gender {
    MALE, FEMALE;

    public static Gender from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.GENDER_CANNOT_BE_EMPTY);
        }

        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_GENDER_VALUE);
        }
    }
}
