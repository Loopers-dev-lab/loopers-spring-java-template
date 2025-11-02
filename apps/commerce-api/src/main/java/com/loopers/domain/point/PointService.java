package com.loopers.domain.point;

import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 도메인 서비스.
 * <p>
 * 포인트 생성, 조회, 충전 등의 도메인 로직을 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class PointService {
    private final PointRepository pointRepository;

    /**
     * 새로운 포인트를 생성합니다.
     *
     * @param user 포인트 소유자
     * @param balance 초기 잔액
     * @return 생성된 포인트
     */
    public Point create(User user, Long balance) {
        Point point = Point.of(user, balance);
        return pointRepository.save(point);
    }

    /**
     * 사용자 ID로 포인트를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 포인트, 없으면 null
     */
    public Point findByUserId(String userId) {
        return pointRepository.findByUserId(userId);
    }

    /**
     * 사용자의 포인트를 충전합니다.
     * <p>
     * 트랜잭션 내에서 실행되어 데이터 일관성을 보장합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param amount 충전할 금액 (0보다 커야 함)
     * @return 충전된 포인트
     * @throws CoreException 포인트를 찾을 수 없거나 충전 금액이 유효하지 않을 경우
     */
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


