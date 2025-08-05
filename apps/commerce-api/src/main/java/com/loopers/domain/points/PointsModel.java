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



    private Long userId;
    @Embedded
    private Point point;

    public PointsModel() {
    }
    private PointsModel(Long userId, BigDecimal amount) {
        this.userId = userId;
        this.point = new Point(amount);
    }
    public static PointsModel from(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId is null");
        }
        return new PointsModel(userId, BigDecimal.ZERO);
    }
    public static PointsModel from(Long userId, BigDecimal amount) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId is null");
        }
        return new PointsModel(userId, amount);
    }
    public Long getUserId() {
        return userId;
    }
    public BigDecimal getPoint() {
        return point.getAmount();
    }
}
