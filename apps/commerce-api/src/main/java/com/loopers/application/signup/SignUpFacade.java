package com.loopers.application.signup;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SignUpFacade {
    private final UserService userService;

    private final PointService pointService;

    @Transactional
    public SignUpInfo signUp(String userId, String email, String birthDateStr, Gender gender) {
        User user = userService.create(userId, email, birthDateStr, gender);
        pointService.create(user, 0L);
        return SignUpInfo.from(user);
    }
}
