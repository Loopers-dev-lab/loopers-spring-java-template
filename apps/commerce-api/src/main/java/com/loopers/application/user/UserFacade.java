package com.loopers.application.user;

import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.domain.user.embeded.UserLoginId;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserFacade {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserFacade(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public UserCommand.UserResponse createUser(UserV1Dto.CreateUserRequest request) {
        UserCommand.CreateUserRequest command = UserCommand.CreateUserRequest.from(request);
        UserEntity userModel = userService.buildUserModel(command);

        if (isLoginIdExists(userModel.getLoginId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 아이디입니다.");
        }
        return UserCommand.UserResponse.from(userRepository.save(userModel));
    }
    private boolean isLoginIdExists(String loginId) {
        return userRepository.existsByLoginId(new UserLoginId(loginId));
    }
    public UserCommand.UserResponse getUserById(Long userId) {
        if(userId == null) {
            return null;
        }
        return findByUserId(userId)
                .map(UserCommand.UserResponse::from)
                .orElse(null);
    }
    public Optional<UserEntity> findByUserId(Long userId) {
        return userRepository.findById(userId);
    }
    public boolean isUserIdExists(Long userId) {
        return userRepository.existsById(userId);
    }

}
