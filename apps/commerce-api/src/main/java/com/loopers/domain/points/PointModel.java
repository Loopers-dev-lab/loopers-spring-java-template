package com.loopers.domain.points;

import com.loopers.domain.BaseEntity;
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
public class PointModel extends BaseEntity {

    @Column(unique = true, nullable = false, length = 10)
    private String memberId;

    @Column(nullable = false)
    private BigDecimal amount;

    public static PointModel create(String memberId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다");
        }

        return new PointModel(memberId, amount);
    }

    public void addAmount(BigDecimal addAmount) {
        if (addAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("추가하는 포인트는 0보다 커야 합니다");
        }
        this.amount = this.amount.add(addAmount);
    }
}
