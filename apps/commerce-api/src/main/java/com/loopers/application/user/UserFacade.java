package com.loopers.application.user;


import com.loopers.application.point.PointFacade;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {

	private final UserService userService;
    private final PointFacade pointFacade;

    @Transactional
    public UserInfo signUp(String userId, String email, String birthDate, Gender gender) {
        User user = userService.signUp(userId, email, birthDate, gender);
        pointFacade.createPointForUser(userId);
        return UserInfo.from(user);
    }

    public UserInfo getUserInfo(String userId) {
        User user = userService.getUserByUserId(userId);
        return UserInfo.from(user);
    }
}
