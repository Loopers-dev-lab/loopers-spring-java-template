package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;

    public Point findByUserId(String userId) {
      return pointRepository.findByUserId(userId);
    }


    @Transactional
    public Point charge(String userId, Long chargeAmount) {
        Point point = pointRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "포인트 정보를 찾을 수 없습니다."));
        point.charge(chargeAmount);
        return pointRepository.save(point);
    }
}
