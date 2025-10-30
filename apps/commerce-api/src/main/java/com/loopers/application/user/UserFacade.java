package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo getUser(String userId) {
        UserModel user = userService.getUser(userId);
        return UserInfo.from(user);
    }

    public UserInfo signUp(String userId, String email, String birthDate, Gender gender) {
        return userService.register(userId, email, birthDate, gender);

    }

}
