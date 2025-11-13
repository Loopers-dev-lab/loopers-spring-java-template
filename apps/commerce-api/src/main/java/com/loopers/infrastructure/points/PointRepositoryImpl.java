package com.loopers.infrastructure.points;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.points.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<PointModel> findByMemberId(String memberId) {
        return pointJpaRepository.findByMemberId(memberId);
    }

    @Override
    public PointModel save(PointModel pointModel) {
        return pointJpaRepository.save(pointModel);
    }
}