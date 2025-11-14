
package com.loopers.domain.point;

import com.loopers.domain.user.User;
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
  public Point getAvailablePoints(Long userId) {
    return pointRepository.findByUserId(userId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));
  }

  @Transactional(readOnly = true)
  public BigDecimal getAmount(Long userId) {
    return pointRepository.findByUserId(userId)
        .map(Point::getAmount)
        .orElse(null);
  }

  @Transactional
  public BigDecimal charge(User user, BigDecimal chargeAmt) {
    Optional<Point> pointOpt = pointRepository.findByUserIdForUpdate(user.getId());
    if (!pointOpt.isPresent()) {
      throw new CoreException(ErrorType.NOT_FOUND, "현재 포인트 정보를 찾을수 없습니다.");
    }
    pointOpt.get().charge(chargeAmt);
    pointRepository.save(pointOpt.get());
    return getAmount(user.getId());
  }

  @Transactional
  public BigDecimal use(User user, BigDecimal useAmt) {
    Optional<Point> pointOpt = pointRepository.findByUserIdForUpdate(user.getId());
    if (!pointOpt.isPresent()) {
      throw new CoreException(ErrorType.NOT_FOUND, "현재 포인트 정보를 찾을수 없습니다.");
    }
    pointOpt.get().use(useAmt);
    pointRepository.save(pointOpt.get());
    return getAmount(user.getId());
  }
}
