package com.loopers.domain.points;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointsService {
    public BigDecimal chargePoints(PointsModel pointsModel, BigDecimal chargeAmount) {
        if (chargeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "0이하로 포인트를 충전할 수 없습니다.");
        }
        return pointsModel.getPoint().add(chargeAmount);
    }
    
    public BigDecimal deductPoints(PointsModel pointsModel, BigDecimal deductAmount) {
        if (deductAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "0 이하로 포인트를 차감할 수 없습니다.");
        }
        
        BigDecimal currentPoints = pointsModel.getPoint();
        if (currentPoints.compareTo(deductAmount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액이 부족합니다.");
        }
        
        return currentPoints.subtract(deductAmount);
    }
    
    public boolean hasEnoughPoints(PointsModel pointsModel, BigDecimal requiredAmount) {
        return pointsModel.getPoint().compareTo(requiredAmount) >= 0;
    }
}
