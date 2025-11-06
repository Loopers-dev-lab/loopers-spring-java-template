package com.loopers.application.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.UserModel;
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
        UserModel userModel = userService.getUser(userModelId);
        if (Objects.isNull(userModel)) {
            return null;
        }

        PointModel pointModel = pointService.getPointByUserModelId(userModel.getId());
        if (Objects.isNull(pointModel)) {
            return PointInfo.from(pointService.createInitPoint(userModel.getId()));
        }
        return PointInfo.from(pointModel);
    }

    @Transactional
    public Long charge(final PointInfo pointInfo) {
        if (Objects.isNull(pointInfo.userModelId())) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        UserModel userModel = userService.getUser(pointInfo.userModelId());
        if (Objects.isNull(userModel)) {
            throw new CoreException(ErrorType.NOT_FOUND);
        }

        PointModel pointModel = Optional.ofNullable(
                pointService.getPointByUserModelId(userModel.getId())
        ).orElseGet(() -> pointService.createInitPoint(userModel.getId()));

        return pointService.charge(pointModel, pointInfo.point());
    }
}
