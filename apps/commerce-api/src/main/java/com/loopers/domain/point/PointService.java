
package com.loopers.domain.point;

import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointService {

  private final PointRepository pointRepository;

  @Transactional(readOnly = true)
  public BigDecimal getAmount(String userId) {
    return pointRepository.findByUserId(userId)
        .map(PointModel::getAmount)
        .orElse(null);
  }

  @Transactional
  public BigDecimal charge(UserModel user, BigDecimal chargeAmt) {
    Optional<PointModel> pointOpt = pointRepository.findByUserIdForUpdate(user.getUserId());
    if (pointOpt.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND, "현재 포인트 정보를 찾을수 없습니다.");
    }
    pointOpt.get().charge(chargeAmt);
    pointRepository.save(pointOpt.get());
    return getAmount(user.getUserId());
  }

  @Transactional
  public BigDecimal use(UserModel user, BigDecimal useAmt) {
    Optional<PointModel> pointOpt = pointRepository.findByUserIdForUpdate(user.getUserId());
    if (pointOpt.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND, "현재 포인트 정보를 찾을수 없습니다.");
    }
    pointOpt.get().use(useAmt);
    pointRepository.save(pointOpt.get());
    return getAmount(user.getUserId());
  }
}
