package com.loopers.application.point;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointFacade {
    private final PointService pointService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Long getCurrentPoint(String userId) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return pointService.getCurrentPoint(user.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public Long chargePoint(String userId, int amount) {
        User user = userService.findByUserIdForUpdate(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return pointService.chargePoint(user.getId(), amount);
    }
}
