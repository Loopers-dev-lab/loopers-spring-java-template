package com.loopers.application.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.user.UserCreateV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {
  private final UserService userService;

  public UserInfo join(UserCreateV1Dto.UserRequest requestDto) {
    User user = userService.join(User.create(requestDto.userId(), requestDto.email(), requestDto.birthday(), requestDto.gender()));
    return UserInfo.from(user);
  }

  public UserInfo getUser(String userId) {
    User user = userService.getUser(userId);
    return UserInfo.from(user);
  }

}
