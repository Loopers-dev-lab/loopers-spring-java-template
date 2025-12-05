package com.loopers.domain.point;


import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Embedded
    private PointBalance balance;

    @Version
    private Long version;

    private Point(Long userId, PointBalance balance) {
        validateUserId(userId);
        this.userId = userId;
        this.balance = balance;
    }

    public static Point create(Long userId) {
        return new Point(userId, PointBalance.zero());
    }

    public static Point create(Long userId, Long initialAmount) {
        return new Point(userId, PointBalance.of(initialAmount));
    }

    public void charge(Long amount) {
        this.balance = this.balance.charge(amount);
    }

    public void use(Long amount) {
        this.balance = this.balance.use(amount);
    }

    public Long getBalanceValue() {
        return this.balance.getValue();
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }
}
