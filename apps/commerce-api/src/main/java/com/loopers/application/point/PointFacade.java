package com.loopers.application.point;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
    return pointService.getAmount(user.getUserId());
  }

  public BigDecimal charge(String userId, BigDecimal chargeAmt) {
    UserModel user = userService.getUser(userId);
    if (user == null) throw new CoreException(ErrorType.NOT_FOUND, "유저정보를 찾을수 없습니다.");
    return pointService.charge(user, chargeAmt);
  }

}
