package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.point.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {
  private final PointJpaRepository jpaRepository;

  @Override
  public PointModel save(PointModel point) {
    return jpaRepository.save(point);
  }

  @Override
  public Optional<PointModel> findByUserId(String userId) {
    return jpaRepository.findByUserUserId(userId);
  }

  @Override
  public Optional<PointModel> findByUserIdForUpdate(String userId) {
    return jpaRepository.findByUserUserId(userId);
  }

}
