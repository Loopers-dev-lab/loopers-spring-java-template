package com.loopers.domain.point;

import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PointService {
    private final PointRepository pointRepository;

    @Transactional
    public void createPoint(Long userId) {
        Point point = Point.create(userId);
        pointRepository.save(point);
    }

    @Transactional
    public Long chargePoint(Long userId, int amount) {
        Point point = pointRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));
        point.charge(amount);
        pointRepository.save(point);
        return point.getAmount();
    }

    @Transactional(readOnly = true)
    public Optional<Long> getCurrentPoint(Long userId) {
        return pointRepository.findByUserId(userId).map(Point::getAmount);
    }

    @Transactional
    public void checkAndDeductPoint(Long userId, Price price) {
        Point point = pointRepository.findByUserIdForUpdate(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.")
        );
        point.deduct(price.amount());
        pointRepository.save(point);
    }
}
