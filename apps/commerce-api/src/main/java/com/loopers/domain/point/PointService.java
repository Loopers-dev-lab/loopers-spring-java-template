package com.loopers.domain.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class PointService {
    private final PointRepository pointRepository;

    public Point getPointByUserModelId(final Long userModelId) {
        return pointRepository.findByUserModelId(userModelId)
                .orElse(null);
    }

    @Transactional
    public Point createInitPoint(final Long userModelId) {
        return pointRepository.save(Point.create(userModelId));
    }

    @Transactional
    public Long charge(final Point pointModel, final Long point) {
        return pointModel.charge(point);
    }
}
