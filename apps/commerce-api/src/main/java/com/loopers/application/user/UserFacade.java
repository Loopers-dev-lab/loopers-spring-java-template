package com.loopers.application.user;

import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserDomainService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserDomainService userDomainService;
    private final PointAccountDomainService pointAccountDomainService;

    public UserInfo getUser(String userId) {
        User user = userDomainService.getUser(userId);

        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 유저 입니다.");
        }

        return UserInfo.from(user);
    }

    public UserInfo signUp(String userId, String email, String birthDate, Gender gender) {

        // 회원가입
        User user = userDomainService.register(userId, email, birthDate, gender);

        // 계좌 생성
        pointAccountDomainService.createForUser(user.getUserId());

        return UserInfo.from(user);
    }

}
