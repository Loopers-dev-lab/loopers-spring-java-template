package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
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
        UserModel userModel = userService.save(info.toModel());
        return UserInfo.from(userModel);
    }

    public UserInfo getUser(final Long id) {
        UserModel userModel = userService.getUser(id);
        if (Objects.isNull(userModel)) {
            return null;

        }
        return UserInfo.from(userModel);
    }
}
