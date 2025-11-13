package com.loopers.application.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;

    public UserInfo accountUser( String userId, String email, String birthdate, Gender gender ) {

        User user = userService.accountUser(userId, email, birthdate, gender);
        return UserInfo.from(user);

    }

    public UserInfo getUserInfo(String userId) {
        User user = userService.getUserByUserId(userId);

        if(user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "해당 ID를 가진 회원이 존재하지 않습니다.");
        }

        return UserInfo.from(user);
    }

    public BigDecimal getUserPoint(String userId) {
        User findUser = userService.getUserPointByUserId(userId);

        if (findUser == null ) {
            throw new CoreException(ErrorType.NOT_FOUND, "해당 ID를 가진 회원이 존재하지 않습니다.");
        }

        return findUser.getPoint().getAmount();
    }

    public UserInfo chargeUserPoint(String userId, BigDecimal chargePoint) {
        User findUser = userService.chargePointByUserId(userId, chargePoint);

        return UserInfo.from(findUser);
    }
}
