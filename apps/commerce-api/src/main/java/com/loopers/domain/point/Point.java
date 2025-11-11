package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "tb_point")
@Getter
public class Point extends BaseEntity {
    @Column(name = "user_id", nullable = false, updatable = false, unique = true)
    private Long userId;
    private Long amount;

    protected Point() {
    }

    private Point(Long userId, Long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public static Point create(Long userId) {
        return new Point(userId, 0L);
    }

    public void charge(int amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 포인트는 0보다 커야 합니다.");
        }

        this.amount += amount;
    }
}
