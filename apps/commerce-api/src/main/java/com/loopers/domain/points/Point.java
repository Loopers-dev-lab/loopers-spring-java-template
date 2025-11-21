package com.loopers.domain.points;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "point")
public class Point extends BaseEntity {

    @Column(unique = true, nullable = false, length = 10)
    private String memberId;

    @Column(nullable = false)
    private BigDecimal amount;

    public static Point create(String memberId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0 이상이어야 합니다");
        }

        return new Point(memberId, amount);
    }

    public void addAmount(BigDecimal addAmount) {
        if (addAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "추가하는 포인트는 0보다 커야 합니다");
        }
        this.amount = this.amount.add(addAmount);
    }

    public void pay(BigDecimal payAmount) {
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제할 포인트는 0보다 커야 합니다");
        }
        if (this.amount.compareTo(payAmount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다");
        }
        this.amount = this.amount.subtract(payAmount);
    }

    public boolean canAfford(BigDecimal amount) {
        return this.amount.compareTo(amount) >= 0;
    }
}
