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

    public UserInfo signup(UserCommand.SignupCommand signupCommand) {
        User savedUser = userService.signup(signupCommand);
        return UserInfo.from(savedUser);
    }

    public UserInfo getUserInfo(String loginId) {
        User user = userService.findUserByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, loginId + " 사용자를 찾을 수 없습니다."));

        return UserInfo.from(user);
    }

}
