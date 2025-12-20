package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public Optional<Point> findByUserId(Long userId) {
        return pointRepository.findByUserId(userId);
    }

    @Transactional
    public Optional<Point> savePoint(Point point) {
        return pointRepository.save(point);
    }

    /**
     * 포인트 차감 (동시성 안전)
     * @param userId 사용자 ID (DB PK)
     * @param deductAmount 차감할 금액
     * @throws CoreException 차감량이 0 이하이거나, 포인트가 부족한 경우
     */
    @Transactional(noRollbackFor = CoreException.class)
    public void deduct(Long userId, BigDecimal deductAmount) {
        // 차감량 유효성 검사
        if (deductAmount == null || deductAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "포인트 차감량은 0보다 커야 합니다."
            );
        }

        // QueryDSL로 동시성 안전하게 업데이트
        long updatedRows = pointRepository.deduct(userId, deductAmount);
        
        if (updatedRows == 0L) {
            // 포인트 조회하여 상세 메시지 제공
            Point point = pointRepository.findByUserId(userId)
                    .orElseThrow(() -> new CoreException(
                            ErrorType.NOT_FOUND,
                            "[userId = " + userId + "] Point를 찾을 수 없습니다."
                    ));
            
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "포인트가 부족합니다. (현재 포인트: " + point.getAmount() + ", 요청 금액: " + deductAmount + ")"
            );
        }
    }

    /**
     * 포인트 충전 (동시성 안전)
     * @param userId 사용자 ID (DB PK)
     * @param chargeAmount 충전할 금액
     * @throws CoreException 충전량이 0 이하이거나, Point를 찾을 수 없는 경우
     */
    @Transactional
    public void charge(Long userId, BigDecimal chargeAmount) {
        // 충전량 유효성 검사
        if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "포인트 충전량은 0보다 커야 합니다."
            );
        }

        // QueryDSL로 동시성 안전하게 업데이트
        long updatedRows = pointRepository.chargeAmount(userId, chargeAmount);
        
        if (updatedRows == 0L) {
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "[userId = " + userId + "] 포인트를 충전할 Point 객체를 찾을 수 없습니다."
            );
        }
    }
}
