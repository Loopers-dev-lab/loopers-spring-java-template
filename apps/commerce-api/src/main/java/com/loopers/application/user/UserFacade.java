package com.loopers.application.user;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotNull;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class UserFacade {
    private final UserService userService;
    private final PointService pointService;

    @Transactional
    public UserInfo registerUser(UserRegisterCommand command) {
        // 회원 가입
        UserEntity register = userService.register(command.toDomainRequest());

        return UserInfo.from(register);
    }

    @Transactional(readOnly = true)
    public UserInfo getUserByUsername(@NotNull String username) {
        return Optional.ofNullable(userService.getUserByUsername(username))
                .map(UserInfo::from)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_USER, "user not found for username: " + username));
    }
}
