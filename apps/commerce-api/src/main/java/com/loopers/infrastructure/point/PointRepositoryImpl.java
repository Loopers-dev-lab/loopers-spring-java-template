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

    /**
     * Persist the given Point and return the saved entity.
     *
     * @param point the Point entity to persist
     * @return the saved Point entity
     */
    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }

    /**
     * Retrieve the Point associated with the given user ID.
     *
     * @param userId the user's unique identifier
     * @return an Optional containing the Point for the user if present, {@code Optional.empty()} otherwise
     */
    @Override
    public Optional<Point> findByUserId(String userId) {
        return pointJpaRepository.findByUserId(userId);
    }
}