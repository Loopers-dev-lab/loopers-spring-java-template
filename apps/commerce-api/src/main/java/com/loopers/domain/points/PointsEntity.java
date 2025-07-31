package com.loopers.domain.points;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "point")
public class PointsEntity extends BaseEntity {

    private Long userId;
    @Embedded
    private Point point;

    public PointsEntity() {
    }
    private PointsEntity(Long userId, BigDecimal amount) {
        this.userId = userId;
        this.point = new Point(amount);
    }
    public static PointsEntity from(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId is null");
        }
        return new PointsEntity(userId, BigDecimal.ZERO);
    }
    public void charge(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "0이하로 포인트를 충전할 수 없습니다.");
        }
        this.point = new Point(this.point.getAmount().add(amount));
    }


    public BigDecimal getPoint() {
        return point.getAmount();
    }
}
