package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService userService;

    /**
     * 사용자의 회원가입과 관련된 일련의 비즈니스 로직을 처리한다
     * @param newUser
     * @return
     */
    @Transactional
    public UserInfo joinUser(UserCommand.Create newUser) {
        UserModel resultUser = userService.createUser(newUser); // 실제 domain 계층 처리 결과
        UserInfo userInfo =UserInfo.from(resultUser); // application 레이어용 불변 객체
        // do sth
        return userInfo;
    }

    @Transactional
    public UserInfo getUserInfo(String userId) {
        UserModel model = userService.getUserOrNull(userId);
        if(model == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "이용자ID를 확인해주세요");
        }
        return UserInfo.from(model);
    }
}
