package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserFacade {

    private final UserService userService;

    public UserInfo registerUser(String id, String email, String birthDate, String gender) {
        User user = userService.registerUser(id, email, birthDate, gender);
        return UserInfo.from(user);
    }

    public UserInfo getUserById(String id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return null;
        }
        return UserInfo.from(user);
    }
}
