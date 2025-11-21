package com.loopers.domain.supply.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeConverter;

public record Stock(int quantity) {
    public Stock {
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 음수가 될 수 없습니다.");
        }
    }

    public boolean isOutOfStock() {
        return this.quantity <= 0;
    }

    public Stock decrease(int orderQuantity) {
        if (orderQuantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.");
        }
        if (orderQuantity > this.quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        return new Stock(this.quantity - orderQuantity);
    }

    public static class Converter implements AttributeConverter<Stock, Integer> {

        @Override
        public Integer convertToDatabaseColumn(Stock attribute) {
            return attribute.quantity();
        }

        @Override
        public Stock convertToEntityAttribute(Integer dbData) {
            return new Stock(dbData);
        }
    }
}
