package com.loopers.application.point;

import com.loopers.application.user.UserFacade;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {
    private final PointService pointService;
    private final UserFacade userFacade;

    public PointInfo getBalance(String userId) {
        Point balance = pointService.getBalance(userId);

        return PointInfo.from(userId, balance.amount());
    }

    public PointInfo charge(String userId, long amount) {
        userFacade.getUser(userId);

        Point balance = pointService.charge(userId, amount);

        return  PointInfo.from(userId, balance.amount());
    }
}
