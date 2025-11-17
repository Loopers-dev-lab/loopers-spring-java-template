package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;

    public Optional<Point> findByUserId(Long userId) {
      return pointRepository.findByUserId(userId);
    }

    @Transactional
    public Point charge(Long userId, Long chargeAmount) {
        Point point = pointRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다."));
        point.charge(chargeAmount);
        return pointRepository.save(point);
    }

    @Transactional
    public Point deduct(Long userId, Long deductAmount) {
        Point point = pointRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다."));
        point.deduct(deductAmount);
        return pointRepository.save(point);
    }
}
