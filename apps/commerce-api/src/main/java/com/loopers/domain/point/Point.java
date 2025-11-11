package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal balance;

    @Builder
    private Point(String userId, BigDecimal balance) {
        validateUserId(userId);
        this.userId = userId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User ID는 필수입니다.");
        }
    }

    public void charge(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "충전 금액은 0보다 커야 합니다.");
        }
        this.balance = this.balance.add(amount);
    }

    public void use(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "사용 금액은 0보다 커야 합니다.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "포인트 잔액이 부족합니다. 현재 잔액: " + this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void refund(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "환불 금액은 0보다 커야 합니다.");
        }
        this.balance = this.balance.add(amount);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Point point = (Point) o;
        return Objects.equals(getUserId(), point.getUserId())
            && getBalance().compareTo(point.getBalance()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getBalance().stripTrailingZeros());
    }
}
