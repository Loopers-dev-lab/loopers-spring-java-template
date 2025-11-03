package com.loopers.core.domain.user.type;

import com.loopers.core.domain.error.DomainErrorCode;

import java.util.Arrays;
import java.util.Objects;

public enum UserGender {
    MALE, FEMALE;

    public static UserGender create(String value) {
        Objects.requireNonNull(value, DomainErrorCode.notNullMessage("사용자의 성별"));
        return Arrays.stream(values())
                .filter(gender -> gender.name().equalsIgnoreCase(value))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorCode.GENDER_IS_MALE_OR_FEMALE.getMessage()));
    }
}
