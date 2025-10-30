package com.loopers.application.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.point.PointCreateV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class PointFacade {
  private final UserService userService;
  private final PointService pointService;
  
  public BigDecimal getPoint(String userId) {
    UserModel user = userService.getUser(userId);
    return pointService.getAmount(userId);
  }

}
