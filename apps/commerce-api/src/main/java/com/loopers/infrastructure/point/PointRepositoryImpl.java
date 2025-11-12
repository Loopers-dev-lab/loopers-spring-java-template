package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {

  private final PointJpaRepository pointJpaRepository;

  @Override
  public Point save(Point point) {
    return pointJpaRepository.save(point);
  }

  @Override
  public Optional<Point> findByUserId(Long userId) {
    return pointJpaRepository.findByUserId(userId);
  }

  @Override
  public Optional<Point> findByUserIdWithLock(Long userId) {
    return pointJpaRepository.findByUserIdWithLock(userId);
  }

  @Override
  public Optional<Point> findByUserLoginId(String loginId) {
    return pointJpaRepository.findByUserLoginId(loginId);
  }

  @Override
  public Optional<Point> findByUserLoginIdWithLock(String loginId) {
    return pointJpaRepository.findByUserLoginIdWithLock(loginId);
  }

}
