package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class PointFacade {

    private final PointService pointService;

    // 포인트 조회
    public PointInfo getPoint(String loginId){
        Point point = pointService.findByUserLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[loginId = " + loginId + "] Point를 찾을 수 없습니다."));
        return PointInfo.from(point);
    }

    // 포인트 충전
    public PointInfo charge(String loginId, BigDecimal chargeAmount) {
        Point point = pointService.findByUserLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[loginId = " + loginId + "] 포인트를 충전할 Point 객체를 찾을 수 없습니다."));
        point.charge(chargeAmount);
        Point savedPoint = pointService.savePoint(point)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[loginId = " + loginId + "] 포인트를 충전 후 Point 객체를 찾을 수 없습니다."));
        return PointInfo.from(savedPoint);
    }
}
