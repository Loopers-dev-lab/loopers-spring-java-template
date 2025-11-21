package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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
        Optional<Point> point = pointRepository.findPointByUserId(userId);

        if (point.isPresent()) {
            Point existingPoint = point.get();
            existingPoint.charge(chargePoint);
            return pointRepository.savePoint(existingPoint);
        } else {
            Point newPoint = Point.createPoint(userId);
            newPoint.charge(chargePoint);
            return pointRepository.savePoint(newPoint);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Point> findPointByUserId(Long userId) {
        return pointRepository.findPointByUserId(userId);
    }

    public void deductPoint(Long userId, int amount) {
        Point point = pointRepository.findPointByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트를 찾을 수 없습니다."));

        point.deduct(amount);
        pointRepository.savePoint(point);
    }
}
