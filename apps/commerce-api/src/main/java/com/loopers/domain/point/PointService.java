
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
  public BigDecimal save(Point point) {
    Point saved = pointRepository.save(point);
    return saved.getAmount();
  }

  @Transactional
  public BigDecimal charge(Long userId, BigDecimal chargeAmt) {
    Optional<Point> pointOpt = pointRepository.findByUserIdForUpdate(userId);
    if (!pointOpt.isPresent()) {
      throw new CoreException(ErrorType.NOT_FOUND, "현재 포인트 정보를 찾을수 없습니다.");
    }
    pointOpt.get().charge(chargeAmt);
    Point saved = pointRepository.save(pointOpt.get());
    return saved.getAmount();
  }

  @Transactional
  public BigDecimal use(Long userId, BigDecimal useAmt) {
    Optional<Point> pointOpt = pointRepository.findByUserIdForUpdate(userId);
    if (!pointOpt.isPresent()) {
      throw new CoreException(ErrorType.NOT_FOUND, "현재 포인트 정보를 찾을수 없습니다.");
    }
    Point point = pointOpt.get();
    point.use(useAmt);
    Point saved = pointRepository.save(point);
    return saved.getAmount();
  }
}
