package com.loopers.domain.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointService {

    private final PointRepository pointRepository;

    public Optional<Point> findByUserLoginId(String loginId) {
        return pointRepository.findByUser_loginId(loginId);
    }

    public Optional<Point> savePoint(Point point) {
        return pointRepository.save(point);
    }
}
