package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.interfaces.api.point.PointV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {
    private final PointService pointService;

    public PointInfo getPoint(String userId) {
        Point point = pointService.findPointByUserId(userId);

        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자 존재하지 않습니다.");
        }
        return PointInfo.from(point);
    }

    public PointInfo chargePoint(PointV1Dto.ChargePointRequest request) {
        return PointInfo.from(pointService.chargePoint(request.userId(), request.chargeAmount()));
    }
}
