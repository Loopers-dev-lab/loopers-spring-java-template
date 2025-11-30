package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointBalance {

    @Column(name = "balance", nullable = false)
    private Long value;

    private PointBalance(Long value) {
        validate(value);
        this.value = value;
    }

    public static PointBalance of(Long value) {
        return new PointBalance(value);
    }

    public static PointBalance zero() {
        return new PointBalance(0L);
    }

    private void validate(Long value) {
        if (value == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액은 필수입니다.");
        }
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액은 0 이상이어야 합니다.");
        }
    }

    public PointBalance charge(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
        }
        if (this.value > Long.MAX_VALUE - amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 가능한 최대 금액을 초과했습니다.");
        }
        return new PointBalance(this.value + amount);
    }

    public PointBalance use(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 금액은 0보다 커야 합니다.");
        }
        if (this.value < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "보유 포인트가 부족합니다.");
        }
        return new PointBalance(this.value - amount);
    }
}
