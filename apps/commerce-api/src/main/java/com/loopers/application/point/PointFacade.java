package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;
    private final UserService userService;

    @Transactional
    public void createPointForUser(Long userId) {
        pointService.createPoint(userId);
    }

    @Transactional(readOnly = true)
    public Point getPointByUserBusinessId(String userBusinessId) {
        User user = userService.getUserByUserId(userBusinessId);
        return pointService.getPoint(user.getId());
    }

    @Transactional(readOnly = true)
    public Point getPoint(Long userId) {
        return pointService.getPoint(userId);
    }

    @Transactional
    public void chargePoint(PointCommand command) {
        User user = userService.getUserByUserId(command.userBusinessId());

        pointService.chargePoint(
                user.getId(),
                command.amount()
        );
    }
}
