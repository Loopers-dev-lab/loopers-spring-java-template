package com.loopers.application.pointwallet;

import com.loopers.domain.user.Point;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 포인트 지갑 파사드.
 * <p>
 * 포인트 조회 및 충전 유즈케이스를 처리하는 애플리케이션 서비스입니다.
 * 트랜잭션 경계를 관리하여 데이터 일관성을 보장합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class PointWalletFacade {
    private final UserRepository userRepository;

    /**
     * 사용자의 포인트를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 포인트 정보
     * @throws CoreException 사용자를 찾을 수 없는 경우
     */
    public PointsInfo getPoints(String userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return PointsInfo.from(user);
    }

    /**
     * 사용자의 포인트를 충전합니다.
     * <p>
     * 트랜잭션 내에서 실행되어 데이터 일관성을 보장합니다.
     * </p>
     *
     * @param userId 충전할 사용자 ID
     * @param amount 충전할 포인트 금액
     * @return 충전된 포인트 정보
     * @throws CoreException 사용자를 찾을 수 없거나 충전 금액이 유효하지 않은 경우
     */
    @Transactional
    public PointsInfo chargePoint(String userId, Long amount) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        Point point = Point.of(amount);
        user.receivePoint(point);
        User savedUser = userRepository.save(user);
        return PointsInfo.from(savedUser);
    }

    /**
     * 포인트 정보를 담는 레코드.
     *
     * @param userId 사용자 ID
     * @param balance 포인트 잔액
     */
    public record PointsInfo(String userId, Long balance) {
        /**
         * User 엔티티로부터 PointsInfo를 생성합니다.
         *
         * @param user 사용자 엔티티
         * @return 생성된 PointsInfo
         */
        public static PointsInfo from(User user) {
            return new PointsInfo(user.getUserId(), user.getPoint().getValue());
        }
    }
}

