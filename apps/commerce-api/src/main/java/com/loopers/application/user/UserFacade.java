package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo createUser(String id, String email, String birthDate, String gender) {
        UserModel user = userService.createUser(id, email, birthDate, gender);
        return UserInfo.from(user);
    }

    public UserInfo getUserById(String id) {
        UserModel user = userService.getUserById(id);
        return user != null ? UserInfo.from(user) : null;
    }

    public Integer getUserPoint(String id) {
        return userService.getUserPoint(id);
    }

    public UserInfo chargePoint(String id, Integer amount) {
        UserModel user = userService.chargePoint(id, amount);
        return UserInfo.from(user);
    }
}
