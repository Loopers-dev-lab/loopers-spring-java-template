package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    public UserResult register(String userId, String email, LocalDate birth, Gender gender) {
        User user = userService.registerUser(userId, email, birth, gender);
        return UserResult.from(user);
    }

    public UserResult getUser(String userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return UserResult.from(user);
    }
}
