package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.user.UserV1Dto;
import org.springframework.stereotype.Component;

@Component
public class UserFacade {
    private final UserService userService;

    public UserFacade(UserService userService) {
        this.userService = userService;
    }

    public UserCommand.UserResponse createUser(UserV1Dto.CreateUserRequest request) {
        UserCommand.CreateUserRequest command = UserCommand.CreateUserRequest.from(request);
        UserModel saved = userService.save(command);
        return UserCommand.UserResponse.from(saved);
    }
    public UserCommand.UserResponse getUserById(Long userId) {
        if(userId == null) {
            return null;
        }
        return userService.findByUserId(userId)
                .map(UserCommand.UserResponse::from)
                .orElse(null);
    }
}
