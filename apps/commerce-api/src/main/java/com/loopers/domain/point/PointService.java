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

    /**
     * Retrieve the Point associated with the given user ID.
     *
     * @param userId the identifier of the user whose Point is requested
     * @return the user's Point if present, otherwise {@code null}
     */
    @Transactional(readOnly = true)
    public Point getPointByUserId(String userId) {
        return pointRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Create and persist a new Point for the given user.
     *
     * @param userId the identifier of the user who will own the point balance
     * @param initialAmount the initial point amount to assign to the new Point
     * @return the persisted Point entity
     */
    @Transactional
    public Point createPoint(String userId, Long initialAmount) {
        Point point = Point.create(userId, initialAmount);
        return pointRepository.save(point);
    }

    /**
     * Applies a point charge to the user's point balance, creating a point record with zero balance if one does not exist.
     *
     * @param userId the identifier of the user receiving the charge
     * @param amount the amount to add to the user's point balance
     * @return the persisted Point after the charge has been applied
     * @throws CoreException if no user exists with the given userId (ErrorType.NOT_FOUND)
     */
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
}