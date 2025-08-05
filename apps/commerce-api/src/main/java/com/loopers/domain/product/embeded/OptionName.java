package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class OptionName {
    private String name;

    private OptionName(String name) {
        this.name = name;
    }

    public OptionName() {
    }

    public static OptionName of(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션명은 비어있을 수 없습니다.");
        }
        if (name.length() > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션명은 100자를 초과할 수 없습니다.");
        }
        return new OptionName(name.trim());
    }

    public String getValue() {
        return this.name;
    }
}