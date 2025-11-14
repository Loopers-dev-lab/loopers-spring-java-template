package com.loopers.application.point;

import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class PointFacade {
  private final UserService userService;
  private final PointService pointService;

  public BigDecimal getPoint(Long userId) {
    User user = userService.getActiveUser(userId);
    return user.getPoint().getAmount();
  }

  public BigDecimal charge(Long userId, BigDecimal chargeAmt) {
    User user = userService.getActiveUser(userId);
    return pointService.charge(user, chargeAmt);
  }

}
