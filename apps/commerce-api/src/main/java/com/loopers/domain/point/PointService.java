package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public Point findPointByUserId(String userId) {
        return pointRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Point chargePoint(String userId, Long chargeAmount) {
        Point point = pointRepository.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트를 충전할수 없는 사용자입니다."));
        point.charge(chargeAmount);
        return pointRepository.save(point);
    }

    @Transactional
    public Point usePoint(String userId, Long useAmount) {
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));

        if (useAmount == null || useAmount <= 0) {
            throw new CoreException(ErrorType.NOT_FOUND, "차감할 포인트는 1 이상이어야 합니다.");
        }

        if (point.getBalance() < useAmount) {
            throw new CoreException(ErrorType.NOT_FOUND, "포인트가 부족합니다.");
        }

        point.use(useAmount);
        return pointRepository.save(point);
    }
}
