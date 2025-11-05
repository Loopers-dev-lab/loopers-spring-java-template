package com.loopers.domain.point;


import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false, length = 10)
    private String userId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    private Point(String userId, Long amount) {
        validateUserId(userId);
        validateAmount(amount);
        this.userId = userId;
        this.amount = amount;
    }

    public static Point create(String userId) {
        return new Point(userId, 0L);
    }

    public static Point create(String userId, Long initialAmount) {
        return new Point(userId, initialAmount);
    }

    public void charge(Long amount) {
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 금액은 필수입니다.");
        }
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
        }
        this.amount += amount;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 비어있을 수 없습니다.");
        }
    }

    private void validateAmount(Long amount) {
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 금액은 필수입니다.");
        }
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 금액은 0 이상이어야 합니다.");
        }
    }
}
