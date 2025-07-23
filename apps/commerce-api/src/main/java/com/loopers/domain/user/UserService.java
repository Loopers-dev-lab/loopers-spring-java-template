package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.user.embeded.UserLoginId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity save(UserCommand.CreateUserRequest userCommand) {
        UserEntity registerUser = UserEntity.register(
                userCommand.loginId(),
                userCommand.email(),
                userCommand.birth(),
                userCommand.gender());
        if (isLoginIdExists(registerUser.getLoginId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 아이디입니다.");
        }

        return userRepository.save(registerUser);
    }
    public boolean isUserIdExists(Long userId) {
        return userRepository.existsById(userId);
    }
    public boolean isLoginIdExists(String loginId) {
        // JPA를 잘 사용해보지 않아서 질문 new UserLoginId(loginId)로 만들어서 확인하는건가?
        return userRepository.existsByLoginId(new UserLoginId(loginId));
    }
    public Optional<UserEntity> findByUserId(Long userId) {
        return userRepository.findById(userId);
    }
}
