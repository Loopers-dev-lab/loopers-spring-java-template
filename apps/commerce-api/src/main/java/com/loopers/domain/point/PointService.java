package com.loopers.domain.point;

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
}
