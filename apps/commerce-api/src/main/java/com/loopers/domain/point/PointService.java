package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;

    @Transactional
    public Point create(String userId){
        Point point = new Point(userId, 0L);
        return pointRepository.save(point);
    }

    @Transactional(readOnly = true)
    public Point getPoints(String userId){
        return pointRepository.findByUserId(userId)
                .orElse(null);
    }

    @Transactional
    public Point chargePoints(String userId, Long amount){
        Point point = pointRepository.findByUserId(userId)  
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 ID입니다."));
        point.chargePoints(amount);
        return pointRepository.save(point);
    }
}
