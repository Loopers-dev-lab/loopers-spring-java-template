package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {
    private final PointService pointService;

    public PointInfo getBalance(String userId) {
        Point balance = pointService.getBalance(userId);

        if (balance == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 아이디 입니다.");
        }

        return PointInfo.from(userId, balance.amount());
    }

}
