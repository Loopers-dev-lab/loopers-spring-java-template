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

    /**
     * Create a PointModel for the given member with the specified amount.
     *
     * @param memberId the member identifier (up to 10 characters)
     * @param amount the initial point amount; must be greater than or equal to zero
     * @return a new PointModel initialized with the provided memberId and amount
     * @throws IllegalArgumentException if {@code amount} is {@code null} or less than zero
     */
    public static PointModel create(String memberId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 합니다");
        }

        return new PointModel(memberId, amount);
    }

    /**
     * Increases the stored point amount by the specified positive value.
     *
     * @param addAmount the amount to add; must be greater than zero
     * @throws IllegalArgumentException if {@code addAmount} is less than or equal to zero
     */
    public void addAmount(BigDecimal addAmount) {
        if (addAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("추가하는 포인트는 0보다 커야 합니다");
        }
        this.amount = this.amount.add(addAmount);
    }
}