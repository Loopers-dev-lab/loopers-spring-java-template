package com.loopers.application.user;

import com.loopers.domain.user.Email;
import com.loopers.domain.user.UserId;
import com.loopers.domain.user.BirthDate;
import com.loopers.domain.user.Gender;
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

    public UserInfo signup(String userId, String email, String gender, String birthDate) {
        UserModel userModel = new UserModel(new UserId(userId), new Email(email), new Gender(gender), new BirthDate(birthDate));
        UserModel savedUser = userService.signUp(userModel);
        return UserInfo.from(savedUser);
    }

    public UserInfo getUser(UserId userId) {
        UserModel user = userService.getUser(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 요청입니다.");
        }
        return UserInfo.from(user);
    }
}
