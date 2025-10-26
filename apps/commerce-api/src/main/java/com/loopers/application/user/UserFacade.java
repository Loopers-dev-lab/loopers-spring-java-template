package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
    private final UserService exampleService;

    public UserInfo getExample(Long id) {
        UserModel example = exampleService.getExample(id);
        return UserInfo.from(example);
    }
}
