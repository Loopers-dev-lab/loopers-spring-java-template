package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {

    private final PointService pointService;
    private final UserService userService;

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 10)
    )
    public PointInfo chargePoint(PointCommand.ChargeCommand chargeCommand) {
        User user = userService.findUserByLoginId(chargeCommand.loginId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, chargeCommand.loginId() + " 사용자를 찾을 수 없습니다."));

        Point point = pointService.chargePoint(user.getId(), chargeCommand.chargePoint());
        return PointInfo.from(point);
    }

    @Recover
    public PointInfo recoverChargePoint(OptimisticLockingFailureException e, PointCommand.ChargeCommand chargeCommand) {
        throw new CoreException(ErrorType.CONFLICT, "포인트 충전 중 동시성 충돌이 발생했습니다. 다시 시도해주세요.");
    }

    public PointInfo getPointInfo(String loginId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, loginId + " 사용자를 찾을 수 없습니다."));

        Point point = pointService.findPointByUserId(user.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, loginId + " 사용자의 포인트 정보를 찾을 수 없습니다."));

        return PointInfo.from(point);
    }
}
