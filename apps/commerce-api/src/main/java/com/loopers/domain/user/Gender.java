package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum Gender {
    MALE("M", "남성"),
    FEMALE("F", "여성");

    private final String code;
    private final String description;

    Gender(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Gender fromCode(String code) {
        return Arrays.stream(Gender.values())
                .filter(gender -> gender.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, String.format("유효하지 않은 성별 코드입니다. %s", code)));
    }
}

