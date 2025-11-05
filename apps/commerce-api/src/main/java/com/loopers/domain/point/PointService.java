package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;

    @Transactional
    public Point createPoint(String userId) {
        if (pointRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 포인트가 존재하는 사용자입니다.");
        }
        Point point = Point.create(userId);
        return pointRepository.save(point);
    }

    public Point getPoint(String userId) {
        return pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트를 찾을 수 없습니다."));
    }

    public Long getPointAmount(String userId) {
        Point point = getPoint(userId);  // 여기서 예외 발생 가능
        return point.getAmount();
    }

    @Transactional
    public void chargePoint(String userId, Long amount) {
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트를 찾을 수 없습니다."));
        point.charge(amount);
        pointRepository.save(point);
    }
}
