package com.loopers.domain.point;

import com.loopers.domain.common.vo.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;

    public Optional<Money> findPointBalance(Long userId) {
        return pointRepository.findByUserId(userId).map(Point::getBalance);
    }

    public Point getPointOrThrow(Long userId) {
        return pointRepository.findByUserId(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보가 존재하지 않는 유저입니다.")
        );
    }

    public Point getPointWithLock(Long userId) {
        return pointRepository.findByUserIdWithLock(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보가 존재하지 않는 유저입니다.")
        );
    }

    @Transactional
    public Money chargePoint(Long userId, Money amount) {
        Point point = getPointWithLock(userId);
        return point.charge(amount);
    }

    public Point savePoint(Long userId) {
        Point point = Point.create(userId);
        return pointRepository.save(point);
    }

    @Transactional
    public void usePoint(Long userId, Money amount) {
        Point point = getPointWithLock(userId);
        point.use(amount);
    }
}
