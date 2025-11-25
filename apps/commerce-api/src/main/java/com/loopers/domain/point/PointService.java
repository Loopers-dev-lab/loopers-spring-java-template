package com.loopers.domain.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class PointService {
    private final PointRepository pointRepository;

    public Point getPointByUserIdWithLock(final Long userId) {
        return pointRepository.findByUserIdWithLock(userId)
                .orElse(null);
    }

    @Transactional
    public Point createInitPoint(final Long userId) {
        return pointRepository.save(Point.create(userId));
    }

    @Transactional
    public Long charge(final Point pointModel, final Long point) {
        return pointModel.charge(point);
    }
}
