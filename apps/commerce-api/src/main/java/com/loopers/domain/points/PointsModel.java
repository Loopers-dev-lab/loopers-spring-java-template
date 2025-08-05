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
public class PointsModel extends BaseEntity {
    @Embedded
    private UserId userId;
    @Embedded
    private Point point;

    public PointsModel() {
    }
    private PointsModel(UserId userId, Point amount) {
        this.userId = userId;
        this.point = amount;
    }
    public static PointsModel from(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId is null");
        }
        return new PointsModel(new UserId(userId), new Point(BigDecimal.ZERO));
    }
    public static PointsModel from(Long userId, BigDecimal amount) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId is null");
        }
        return new PointsModel(new UserId(userId), new Point(amount));
    }
    public Long getUserId() {
        return this.userId.value();
    }
    public BigDecimal getPoint() {
        return point.value();
    }
}
