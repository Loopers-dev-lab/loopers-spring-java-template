package com.loopers.application.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.domain.user.UserId;
import com.loopers.domain.common.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {
    private final PointService pointService;
    private final UserService userService;

    public PointInfo getPoint(UserId userId) {
        UserModel user = userService.getUser(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 요청입니다.");
        }
        PointModel pointModel = new PointModel(user, new Money(0));
        PointModel point = pointService.findPoint(pointModel);
        
        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "포인트 정보가 없습니다.");
        }
        
        return PointInfo.from(point);
    }

    public PointInfo chargePoint(UserId userId, Money point) {
        UserModel user = userService.getUser(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 요청입니다.");
        }
        PointModel pointModel = new PointModel(user, point);
        pointService.charge(pointModel);
        
        PointModel charged = pointService.findPoint(new PointModel(user, point));
        return PointInfo.from(charged);
    }
}
