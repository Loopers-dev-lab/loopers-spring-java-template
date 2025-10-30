
package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.math.BigDecimal;

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
}
