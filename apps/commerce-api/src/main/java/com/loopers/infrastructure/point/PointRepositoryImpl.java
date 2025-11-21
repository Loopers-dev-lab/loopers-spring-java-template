package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {
  private final PointJpaRepository jpaRepository;

  @Override
  public Point save(Point point) {
    return jpaRepository.saveAndFlush(point);
  }

  @Override
  public Optional<Point> findByUserId(Long userId) {
    return jpaRepository.findByUserId(userId);
  }

  @Override
  public Optional<Point> findByUserLoginId(String userId) {
    return jpaRepository.findByUserLoginId(userId);
  }

  @Override
  public Optional<Point> findByUserIdForUpdate(Long userId) {
    return jpaRepository.findByUserIdForUpdate(userId);
  }
}
