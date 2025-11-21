package com.loopers.domain.point;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@Component
@RequiredArgsConstructor
public class PointService {
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 사용자의 포인트 이력을 조회합니다.
     * TODO : 페이징이 필요할 경우 별도 메소드 추가
     *
     * @param username 사용자명
     * @return 포인트 이력 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<PointHistoryEntity> getPointHistories(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_USER));

        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 사용자에게 포인트를 충전합니다.
     *
     * @param username 사용자명
     * @param amount   충전할 금액
     * @return 충전 후 포인트 잔액
     */
    @Transactional
    public BigDecimal charge(String username, BigDecimal amount) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_USER, "존재하지 않는 사용자입니다."));

        user.chargePoint(amount);

        // 포인트 이력 생성
        PointHistoryEntity history = PointHistoryEntity.createChargeHistory(user, amount, user.getPointAmount());
        pointHistoryRepository.save(history);

        userRepository.save(user);

        return user.getPointAmount();
    }

    /**
     * 사용자의 포인트를 사용합니다.
     *
     * @param user   사용자명
     * @param amount 사용할 금액
     * @return 사용 후 포인트 잔액
     */
    @Transactional
    public BigDecimal use(UserEntity user, BigDecimal amount) {
        user.usePoint(amount);

        // 포인트 이력 생성 (사용)
        PointHistoryEntity history = PointHistoryEntity.createUseHistory(user, amount, user.getPointAmount());
        pointHistoryRepository.save(history);

        userRepository.save(user);

        return user.getPointAmount();
    }
}
