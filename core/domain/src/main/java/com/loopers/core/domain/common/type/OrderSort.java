package com.loopers.core.domain.common.type;

import java.util.Arrays;
import java.util.Objects;

public enum OrderSort {
    ASC, DESC, NONE;

    public static OrderSort from(String value) {
        if (Objects.isNull(value)) return NONE;

        return Arrays.stream(values())
                .filter(orderSort -> orderSort.name().equalsIgnoreCase(value))
                .findAny()
                .orElse(NONE);
    }
}
