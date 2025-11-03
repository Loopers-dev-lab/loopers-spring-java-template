package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> findById(String id) {
        return pointJpaRepository.findById(id);
    }

    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }
}
