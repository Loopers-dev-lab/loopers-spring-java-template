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
    public Point getPointByLoginId(String loginId) {
        User user = userService.getUserByLoginId(loginId);
        return pointService.getPoint(user.getId());
    }

    @Transactional(readOnly = true)
    public Point getPoint(Long userId) {
        return pointService.getPoint(userId);
    }

    @Transactional
    public void chargePoint(PointCommand command) {
        User user = userService.getUserByLoginId(command.loginId());

        pointService.chargePoint(
                user.getId(),
                command.amount()
        );
    }
}
