package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_accounts")
public class PointAccount extends BaseEntity {

    private String userId;

    @Embedded
    private Point balance = Point.zero();

    protected PointAccount() {
    }

    protected PointAccount(String userId) {
        this.userId = userId;
    }

    public static PointAccount create(String userId) {
        return new PointAccount(userId);
    }

    public void charge(long amount) {
        validateAmount(amount);

        this.balance = Point.of(this.balance.amount() + amount);
    }

    public void deduct(long amount) {
        if (amount <= 0L) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감할 포인트는 1원 이상이어야 합니다.");
        }

        if (this.balance.amount() < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }

        this.balance = Point.of(this.balance.amount() - amount);
    }

    private static void validateAmount(long amount) {
        if (amount <= 0L) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 1원 이상 충전 가능합니다.");
        }
    }

    public Point getBalance() {
        return balance;
    }

}
