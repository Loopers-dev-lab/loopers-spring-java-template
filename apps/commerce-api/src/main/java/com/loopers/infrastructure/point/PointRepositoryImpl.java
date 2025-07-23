package com.loopers.infrastructure.point;

import com.loopers.domain.points.PointsEntity;
import com.loopers.domain.points.PointsRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PointRepositoryImpl implements PointsRepository {
    private final PointJpaRepository pointJpaRepository;

    public PointRepositoryImpl(PointJpaRepository pointJpaRepository) {
        this.pointJpaRepository = pointJpaRepository;
    }

    @Override
    public PointsEntity save(PointsEntity pointsModel) {
        return pointJpaRepository.save(pointsModel);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return pointJpaRepository.existsById(userId);
    }

    @Override
    public Optional<PointsEntity> findByUserId(Long userId) {
        return pointJpaRepository.findById(userId);
    }

}
