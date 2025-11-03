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
    public Point create(String id){
        Point point = new Point(id, 0L);
        return pointRepository.save(point);
    }

    @Transactional(readOnly = true)
    public Point getPoints(String id){
        return pointRepository.findById(id)
                .orElse(null);
    }

    @Transactional
    public Point chargePoints(String id, Long amount){
        Point point = pointRepository.findById(id)  
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 ID입니다."));
        point.chargePoints(amount);
        return pointRepository.save(point);
    }
}
