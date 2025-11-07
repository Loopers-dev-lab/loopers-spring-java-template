package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserInfo signup(User user) {
        User savedUser = userService.register(user);
        return UserInfo.from(savedUser);
    }

    public UserInfo getUser(Long id) {
        User user = userService.findById(id);
        return UserInfo.from(user);
    }
}
