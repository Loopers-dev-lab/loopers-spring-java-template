package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.loopers.support.error.ErrorMessages.INVALID_POINT_AMOUNT;

@Entity
@Table(name = "point")
@Getter
@NoArgsConstructor
public class Point extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "point_amount", nullable = false)
    private BigDecimal pointAmount;

    @Builder
    public Point(String userId, BigDecimal pointAmount){
        validatePointAmount(pointAmount);
        this.userId = userId;
        this.pointAmount = pointAmount;
    }

    private void validatePointAmount(BigDecimal pointAmount) {
        if (pointAmount == null || pointAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, INVALID_POINT_AMOUNT);
        }
    }

    public void chargePoints(BigDecimal pointAmount) {
        if (pointAmount == null || pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, INVALID_POINT_AMOUNT);
        }
        this.pointAmount = this.pointAmount.add(pointAmount);
    }

    public void usePoints(BigDecimal pointAmount) {
        if (pointAmount == null || pointAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, INVALID_POINT_AMOUNT);
        }
        this.pointAmount = this.pointAmount.subtract(pointAmount);
    }

    public BigDecimal getPointAmount() {
        return pointAmount;
    }
}
