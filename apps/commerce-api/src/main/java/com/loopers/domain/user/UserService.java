package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import org.springframework.stereotype.Component;

@Component
public class UserService {

    public UserModel buildUserModel(UserCommand.CreateUserRequest userCommand) {
        return UserModel.register(
                userCommand.loginId(),
                userCommand.email(),
                userCommand.birth(),
                userCommand.gender());
    }

}
