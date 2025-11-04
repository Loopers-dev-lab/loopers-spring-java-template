package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> findByUserId(Long userId) {
        return pointJpaRepository.findByUserId(userId);
    }

    @Override
    public Point save(Point point) {
        try {
            return pointJpaRepository.save(point);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.CONFLICT, "이미 포인트가 존재하는 유저입니다.");
        }
    }
}
