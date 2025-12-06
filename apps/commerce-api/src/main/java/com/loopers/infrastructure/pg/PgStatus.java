package com.loopers.infrastructure.pg;

public enum PgStatus {
    SUCCESS,
    FAILED,
    PENDING,
    UNKNOWN;

    public static PgStatus from(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
