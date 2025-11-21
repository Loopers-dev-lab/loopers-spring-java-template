package com.loopers.application.user;


import com.loopers.application.point.PointFacade;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFacade {

	private final UserService userService;
    private final PointFacade pointFacade;

    @Transactional
    public UserInfo signUp(UserCommand command) {
        User user = userService.signUp(
                command.userId(),
                command.email(),
                command.birthDate(),
                command.gender()
        );
        pointFacade.createPointForUser(user.getUserId());
        return UserInfo.from(user);
    }

    public UserInfo getUserInfo(String userId) {
        User user = userService.getUserByUserId(userId);
        return UserInfo.from(user);
    }
}
