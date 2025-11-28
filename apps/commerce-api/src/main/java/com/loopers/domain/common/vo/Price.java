package com.loopers.domain.common.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeConverter;

public record Price(int amount) {
    public Price {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 음수가 될 수 없습니다.");
        }
    }

    public Price add(Price other) {
        return new Price(this.amount + other.amount);
    }

    public Price deduct(Price other) {
        // 가격은 음수가 될 수 없으므로 0 미만이 되면 0으로 처리
        return new Price(Math.max(0, this.amount - other.amount));
    }

    // multiply 메서드 추가
    public Price multiply(int factor) {
        if (factor <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "곱셈 인자는 1 이상의 자연수여야 합니다.");
        }
        return new Price(this.amount * factor);
    }

    public static class Converter implements AttributeConverter<Price, Integer> {

        @Override
        public Integer convertToDatabaseColumn(Price attribute) {
            return attribute.amount();
        }

        @Override
        public Price convertToEntityAttribute(Integer dbData) {
            return new Price(dbData);
        }
    }
}
