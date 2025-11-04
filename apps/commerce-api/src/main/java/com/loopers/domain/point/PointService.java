package com.loopers.domain.point;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PointService {

    private final PointRepository pointRepository;

    @Transactional
    public Point chargePoint(Long userId, Integer chargePoint) {
        Optional<Point> point = pointRepository.findByUserId(userId);

        if (point.isPresent()) {
            Point existingPoint = point.get();
            existingPoint.charge(chargePoint);
            return existingPoint;
        } else {
            Point newPoint = Point.createPoint(userId);
            newPoint.charge(chargePoint);
            return pointRepository.save(newPoint);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Point> getPointByUserId(Long userId) {
        return pointRepository.findByUserId(userId);
    }
}
