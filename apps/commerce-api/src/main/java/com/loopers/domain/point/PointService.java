package com.loopers.domain.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public Point getPointByUserId(String userId) {
        return pointRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Point createPoint(String userId, Long initialAmount) {
        Point point = Point.create(userId, initialAmount);
        return pointRepository.save(point);
    }
}
