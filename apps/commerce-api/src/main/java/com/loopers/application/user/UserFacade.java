package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo register(String userId, String email, String birth, String gender) {
        User user = userService.register(userId, email, birth, gender);
        return UserInfo.from(user);
    }

    public UserInfo getUser(String userId) {
        User user = userService.findUserByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자 존재하지 않습니다.");
        }
        return UserInfo.from(user);
    }
}
