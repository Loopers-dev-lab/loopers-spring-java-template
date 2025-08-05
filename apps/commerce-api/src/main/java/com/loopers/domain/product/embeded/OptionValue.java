package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class OptionValue {
    private String value;

    private OptionValue(String value) {
        this.value = value;
    }

    public OptionValue() {
    }

    public static OptionValue of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션값은 비어있을 수 없습니다.");
        }
        if (value.length() > 200) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션값은 200자를 초과할 수 없습니다.");
        }
        return new OptionValue(value.trim());
    }

    public String getValue() {
        return this.value;
    }
}