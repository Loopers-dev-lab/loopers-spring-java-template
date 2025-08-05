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
}
