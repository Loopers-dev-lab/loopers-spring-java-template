package com.loopers.core.domain.common.type;

import java.util.Arrays;

public enum OrderSort {
    ASC, DESC;

    public static final OrderSort DEFAULT = DESC;

    public static OrderSort from(String value) {
        return Arrays.stream(values())
                .filter(orderSort -> orderSort.name().equalsIgnoreCase(value))
                .findAny()
                .orElse(DEFAULT);
    }
}
