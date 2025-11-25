package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class PointFacade {

    private final PointService pointService;

    // 포인트 조회
    @Transactional(readOnly = true)
    public PointInfo getPoint(Long userId){
        Point point = pointService.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[userId = " + userId + "] Point를 찾을 수 없습니다."));
        return PointInfo.from(point);
    }

    // 포인트 충전
    @Transactional
    public PointInfo charge(Long userId, BigDecimal chargeAmount) {
        // 동시성 안전하게 포인트 충전
        pointService.charge(userId, chargeAmount);
        
        // 충전 후 최신 포인트 정보 조회
        Point point = pointService.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[userId = " + userId + "] 포인트를 충전 후 Point 객체를 찾을 수 없습니다."));
        return PointInfo.from(point);
    }
}
