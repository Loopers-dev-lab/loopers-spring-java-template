package com.loopers.application.user;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;
    private final PointService pointService;

    @Transactional
    public UserInfo registerUser(String userId, String email, String birthday, String gender) {
        User registeredUser = userService.registerUser(userId, email, birthday, gender);
        pointService.createPoint(registeredUser.getId());
        return UserInfo.from(registeredUser);
    }

    public UserInfo getUserInfo(String userId) {
        Optional<User> user = userService.findByUserId(userId);
        return UserInfo.from(user.orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")));
    }
}
