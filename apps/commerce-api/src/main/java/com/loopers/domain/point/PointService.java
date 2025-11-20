package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PointService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public Point initializePoint(String userId) {
        if (pointRepository.existsByUserId(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "이미 포인트가 초기화된 유저입니다: " + userId);
        }

        Point point = Point.builder()
            .userId(userId)
            .balance(BigDecimal.ZERO)
            .build();

        return pointRepository.save(point);
    }

    @Transactional(readOnly = true)
    public Point getPoint(String userId) {
        return pointRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다: " + userId));
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String userId) {
        return pointRepository.findByUserId(userId)
            .map(Point::getBalance)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다: " + userId));
    }

    @Transactional
    public BigDecimal chargePoint(String userId, BigDecimal amount) {
        Point point = pointRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다: " + userId));

        point.charge(amount);

        PointHistory history = PointHistory.builder()
            .userId(userId)
            .transactionType(PointTransactionType.CHARGE)
            .amount(amount)
            .balanceAfter(point.getBalance())
            .description("포인트 충전")
            .build();
        pointHistoryRepository.save(history);

        return point.getBalance();
    }

    @Transactional
    public BigDecimal usePoint(String userId, BigDecimal amount) {
        Point point = pointRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다: " + userId));

        point.use(amount);

        PointHistory history = PointHistory.builder()
            .userId(userId)
            .transactionType(PointTransactionType.USE)
            .amount(amount)
            .balanceAfter(point.getBalance())
            .description("포인트 사용")
            .build();
        pointHistoryRepository.save(history);

        return point.getBalance();
    }

    @Transactional
    public BigDecimal refundPoint(String userId, BigDecimal amount) {
        Point point = pointRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다: " + userId));

        point.refund(amount);

        PointHistory history = PointHistory.builder()
            .userId(userId)
            .transactionType(PointTransactionType.REFUND)
            .amount(amount)
            .balanceAfter(point.getBalance())
            .description("포인트 환불")
            .build();
        pointHistoryRepository.save(history);

        return point.getBalance();
    }

    @Transactional(readOnly = true)
    public List<PointHistory> getPointHistories(String userId) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
