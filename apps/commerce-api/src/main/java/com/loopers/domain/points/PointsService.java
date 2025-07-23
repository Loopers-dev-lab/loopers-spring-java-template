package com.loopers.domain.points;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointsService {
    private final PointsRepository pointsRepository;

    public PointsService(PointsRepository pointsRepository) {
        this.pointsRepository = pointsRepository;
    }

    public PointsEntity save(PointsEntity user) {
        try {
            return pointsRepository.save(user);
        }catch (Exception e){
            throw new CoreException(ErrorType.INTERNAL_ERROR);
        }
    }
    @NonNull
    public PointsEntity getOrCreatePointsByUserId(Long userId) {
        return pointsRepository.findByUserId(userId).orElseGet(
                () -> PointsEntity.from(userId)
        );
    }

    public PointsEntity chargePoints(Long userId, BigDecimal chargeAmount) {
        PointsEntity point = getOrCreatePointsByUserId(userId);
        point.charge(chargeAmount);
        return save(point);
    }
}
