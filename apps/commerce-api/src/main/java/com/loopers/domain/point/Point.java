package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "point")
@Getter
public class Point extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Integer amount;

    @Builder
    private Point(Long userId, Integer amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public Point() {
    }

    public static Point createPoint(Long id) {
        return Point.builder()
                .userId(id)
                .amount(0)
                .build();
    }

    public void charge(Integer amount) {
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 필수입니다.");
        }
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
        }

        this.amount += amount;
    }
}
