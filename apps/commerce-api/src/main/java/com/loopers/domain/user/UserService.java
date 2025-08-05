package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.user.embeded.UserLoginId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
