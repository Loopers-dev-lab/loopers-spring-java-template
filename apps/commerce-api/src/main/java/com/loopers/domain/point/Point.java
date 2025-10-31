package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Point extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    private User user;

    @Column(name = "balance", nullable = false)
    private Long balance;

    public Point(User user, Long balance) {
        this.user = user;
        this.balance = balance != null ? balance : 0L;
    }

    public static Point of(User user, Long balance) {
        return new Point(user, balance);
    }

    public void charge(Long amount) {
        validateChargeAmount(amount);
        this.balance += amount;
    }

    private void validateChargeAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0보다 큰 값이어야 합니다.");
        }
    }
}


