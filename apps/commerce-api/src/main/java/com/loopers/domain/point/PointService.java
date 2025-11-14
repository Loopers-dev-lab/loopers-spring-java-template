package com.loopers.domain.point;

import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Point getPointByUserId(String userId) {
        return pointRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Point createPoint(String userId, Long initialAmount) {
        Point point = Point.create(userId, initialAmount);
        return pointRepository.save(point);
    }

    @Transactional
    public Point charge(String userId, Long amount) {
        // 사용자 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다: " + userId);
        }
        // 포인트 조회 또는 초기화
        Point point = pointRepository.findByUserId(userId)
                .orElseGet(() -> Point.create(userId, 0L));
        // 충전 (도메인 검증 포함)
        point.add(amount);
        return pointRepository.save(point);
    }

    /**
     * 포인트 차감 (주문 등 사용)
     */
    @Transactional
    public Point consume(String userId, Long amount) {
        if (!userRepository.existsById(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다: " + userId);
        }
        Point point = pointRepository.findByUserId(userId)
                .orElseGet(() -> Point.create(userId, 0L));
        point.deduct(amount);
        return pointRepository.save(point);
    }

    /**
     * 포인트 환불/복구 (주문 취소 등)
     */
    @Transactional
    public Point refund(String userId, Long amount) {
        if (!userRepository.existsById(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다: " + userId);
        }
        Point point = pointRepository.findByUserId(userId)
                .orElseGet(() -> Point.create(userId, 0L));
        point.add(amount);
        return pointRepository.save(point);
    }
}
