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
