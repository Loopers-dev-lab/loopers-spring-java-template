package com.loopers.infrastructure.point;

import com.loopers.domain.points.PointsModel;
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
    public PointsModel save(PointsModel pointsModel) {
        return pointJpaRepository.save(pointsModel);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return pointJpaRepository.existsByUserId(userId);
    }

    @Override
    public Optional<PointsModel> findByUserId(Long userId) {
        return pointJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<PointsModel> findByUserIdForUpdate(Long userId) {
        return pointJpaRepository.findByUserIdForUpdate(userId);
    }

    @Override
    public void deleteAll() {
        pointJpaRepository.deleteAll();
    }

}
