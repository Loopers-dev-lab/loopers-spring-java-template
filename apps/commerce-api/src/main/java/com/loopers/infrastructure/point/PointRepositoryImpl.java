package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;


    @Override
    public Optional<Point> findByUser_loginId(String loginId) {
        return pointJpaRepository.findByUser_loginId(loginId);
    }

    @Override
    public Optional<Point> save(Point point) {
        Point savedPoint = pointJpaRepository.save(point);
        return Optional.of(savedPoint);
    }
}
