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

import static com.loopers.support.error.ErrorMessages.INVALID_POINT_AMOUNT;

@Entity
@Table(name = "point")
@Getter
@NoArgsConstructor
public class Point extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "point_amount", nullable = false)
    private Long pointAmount;

    @Builder
    public Point(String userId, Long pointAmount){
        validatePointAmount(pointAmount);
        this.userId = userId;
        this.pointAmount = pointAmount;
    }

    private void validatePointAmount(Long pointAmount) {
        if(pointAmount == null || pointAmount < 0){
            throw new CoreException(ErrorType.BAD_REQUEST, INVALID_POINT_AMOUNT);
        }
    }

    public void chargePoints(long amount) {
        if (amount <= 0) throw new CoreException(ErrorType.BAD_REQUEST, INVALID_POINT_AMOUNT);
        long updated = Math.addExact(this.pointAmount, amount);
        this.pointAmount = updated;
    }
}
