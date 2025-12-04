package com.loopers.application.user;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.user.UserCreateV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class UserFacade {
  private final UserService userService;
  private final PointService pointService;

  public UserInfo join(UserCreateV1Dto.UserRequest requestDto) {
    User user = userService.join(User.create(requestDto.userId(), requestDto.email(), requestDto.birthday(), requestDto.gender()));
    Point point = Point.create(user, BigDecimal.TEN);
    pointService.save(point);

    return UserInfo.from(user);
  }

  public UserInfo getUser(String userId) {
    User user = userService.getUser(userId);
    return UserInfo.from(user);
  }

}
