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
    
    /**
     * 포인트를 충전한다.
     * 동시성 제어를 위해 낙관적 잠금(@Version) 사용
     */
    public void chargePoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전할 포인트는 0보다 커야 합니다.");
        }
        this.point = new Point(this.point.value().add(amount));
    }
    
    /**
     * 포인트를 차감한다.
     * 동시성 제어를 위해 낙관적 잠금(@Version) 사용
     */
    public void deductPoint(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감할 포인트는 0보다 커야 합니다.");
        }
        
        BigDecimal currentAmount = this.point.value();
        if (currentAmount.compareTo(amount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, 
                "포인트가 부족합니다. 현재: " + currentAmount + ", 필요: " + amount);
        }
        
        this.point = new Point(currentAmount.subtract(amount));
    }
    
    /**
     * 충분한 포인트가 있는지 확인
     */
    public boolean hasEnoughPoint(BigDecimal requiredAmount) {
        if (requiredAmount == null) {
            return true;
        }
        return this.point.value().compareTo(requiredAmount) >= 0;
    }
}
