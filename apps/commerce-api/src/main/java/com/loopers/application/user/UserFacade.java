package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;

    public UserInfo accountUser( String userId, String email, String birthdate, String gender ) {

        UserModel user = userService.accountUser(userId, email, birthdate, gender);
        return UserInfo.from(user);

    }
}
