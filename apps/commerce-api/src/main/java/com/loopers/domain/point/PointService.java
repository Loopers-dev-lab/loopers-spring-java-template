package com.loopers.domain.point;

import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointService {
    private final PointRepository pointRepository;

    public Point create(User user, Long balance) {
        Point point = Point.of(user, balance);
        return pointRepository.save(point);
    }

    public Point findByUserId(String userId) {
        return pointRepository.findByUserId(userId);
    }

    @Transactional
    public Point charge(String userId, Long amount) {
        Point point = pointRepository.findByUserId(userId);
        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "포인트를 찾을 수 없습니다.");
        }
        point.charge(amount);
        return pointRepository.save(point);
    }
}


