package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Money balance;

    private Point(Long userId, Money balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public static Point create(Long userId) {
        return new Point(userId, Money.zero());
    }

    public static Point create(Long userId, Money balance) {
        return new Point(userId, balance);
    }

    public Money charge(Money chargeAmount) {
        if (!chargeAmount.isGreaterThan(Money.zero())) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야합니다.");
        }
        this.balance = this.balance.plus(chargeAmount);
        return this.balance;
    }

    public Money use(Money usedAmount) {
        if (!this.balance.isGreaterThanOrEqual(usedAmount)) {
            throw new IllegalArgumentException("잔여 포인트가 부족합니다.");
        }
        this.balance = this.balance.minus(usedAmount);
        return this.balance;
    }
}
