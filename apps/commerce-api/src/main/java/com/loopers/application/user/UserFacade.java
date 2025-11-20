package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class UserFacade {
    private final UserService userService;

    @Transactional
    public UserInfo signUp(final UserInfo info) {
        if (userService.existsByUserId(info.userId())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 ID입니다.");
        }
        User user = userService.save(info.toModel());
        return UserInfo.from(user);
    }

    public UserInfo getUser(final Long id) {
        User user = userService.getUser(id);
        if (Objects.isNull(user)) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return UserInfo.from(user);
    }
}
