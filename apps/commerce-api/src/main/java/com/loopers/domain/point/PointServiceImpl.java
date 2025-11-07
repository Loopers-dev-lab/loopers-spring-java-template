package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointRepository pointRepository;

    @Override
    public Optional<Integer> findPoint(Long userId) {
        return pointRepository.findByUserId(userId).map(Point::getBalance);
    }

    @Override
    public Point getPointOrThrow(Long userId) {
        return pointRepository.findByUserId(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보가 존재하지 않는 유저입니다.")
        );
    }

    @Transactional
    public int chargePoint(Long userId, int amount) {
        Point point = getPointOrThrow(userId);

        return point.increase(amount);
    }
}
