package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo registerUser(User user) {
        User saved = userService.registerUser(user);
        return UserInfo.from(saved);
    }

    public UserInfo getUser(String userId) {
        User user = userService.getUser(userId);
        return UserInfo.from(user);
    }

    /**
     * @deprecated 포인트 조회는 {@link com.loopers.application.point.PointFacade#getBalance(String)}를 사용하세요.
     */
    @Deprecated
    public BigDecimal getPoint(String userId) {
        return userService.getPoint(userId);
    }

    /**
     * @deprecated 포인트 충전은 {@link com.loopers.application.point.PointFacade#chargePoint(String, BigDecimal)}를 사용하세요.
     */
    @Deprecated
    public BigDecimal chargePoint(String userId, BigDecimal amount) {
        return userService.chargePoint(userId, amount);
    }
}
