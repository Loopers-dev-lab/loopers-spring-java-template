package com.loopers.infrastructure.points;

import com.loopers.domain.points.Point;
import com.loopers.domain.points.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> findByMemberId(String memberId) {
        return pointJpaRepository.findByMemberId(memberId);
    }

    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }
}