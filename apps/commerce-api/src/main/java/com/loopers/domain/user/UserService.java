package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import org.springframework.stereotype.Component;

@Component
public class UserService {

    public UserEntity buildUserModel(UserCommand.CreateUserRequest userCommand) {
        return UserEntity.register(
                userCommand.loginId(),
                userCommand.email(),
                userCommand.birth(),
                userCommand.gender());
    }

}
