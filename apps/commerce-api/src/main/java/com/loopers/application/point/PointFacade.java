package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class PointFacade {
    private final PointService pointService;
    private final UserService userService;

    @Transactional
    public PointInfo getOrCreatePointByUserModelId(final Long userModelId) {
        if (Objects.isNull(userModelId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        User user = userService.getUser(userModelId);
        if (Objects.isNull(user)) {
            return null;
        }

        Point point = pointService.getPointByUserIdWithLock(user.getId());
        if (Objects.isNull(point)) {
            return PointInfo.from(pointService.createInitPoint(user.getId()));
        }
        return PointInfo.from(point);
    }

    @Transactional
    public Long charge(final PointInfo pointInfo) {
        if (Objects.isNull(pointInfo.userModelId())) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        User user = userService.getUser(pointInfo.userModelId());
        if (Objects.isNull(user)) {
            throw new CoreException(ErrorType.NOT_FOUND);
        }

        Point point = Optional.ofNullable(
                pointService.getPointByUserIdWithLock(user.getId())
        ).orElseGet(() -> pointService.createInitPoint(user.getId()));

        return pointService.charge(point, pointInfo.point());
    }
}
