package com.loopers.application.points;

import com.loopers.domain.points.PointsEntity;
import com.loopers.domain.points.PointsService;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointsFacade {
    private final PointsService pointsService;
    private final UserService userService;

    public PointsFacade(PointsService pointsService, UserService userService) {
        this.pointsService = pointsService;
        this.userService = userService;
    }
    public PointsCommand.PointInfo getPointInfo(Long userId) {
        if(!userService.isUserIdExists(userId)){
            return null;
        }
        PointsEntity pointsModel = pointsService.getOrCreatePointsByUserId(userId);
        return PointsCommand.PointInfo.from(pointsModel);
    }
    public PointsCommand.PointInfo chargePoints(Long userId, BigDecimal amount) {
        if(!userService.isUserIdExists(userId)){
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자를 찾을 수 없습니다.");
        }
        PointsEntity entity = pointsService.chargePoints(userId, amount);
        return PointsCommand.PointInfo.from(entity);
    }
}
