package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PointRepository의 JPA 구현체.
 * <p>
 * Spring Data JPA를 활용하여 Point 엔티티의 
 * 영속성 작업을 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository pointJpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point findByUserId(String userId) {
        return pointJpaRepository.findByUserId(userId).orElse(null);
    }
}

