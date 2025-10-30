package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;

    public UserInfo accountUser( String userId, String email, String birthdate, String gender ) {

        UserModel user = userService.accountUser(userId, email, birthdate, gender);
        return UserInfo.from(user);

    }

    public UserInfo getUserInfo(String userId) {
        UserModel user = userService.getUserByUserId(userId);

        if(user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "해당 ID를 가진 회원이 존재하지 않습니다.");
        }

        return UserInfo.from(user);
    }

    public Integer getUserPoint(String userId) {
        return userService.getUserPointByUserId(userId);
    }
}
