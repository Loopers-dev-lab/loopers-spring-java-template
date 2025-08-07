package com.loopers.domain.points;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class PointsService {
    
    private final PointsRepository pointsRepository;

    public PointsService(PointsRepository pointsRepository) {
        this.pointsRepository = pointsRepository;
    }

    @Transactional
    public PointsModel chargePoints(Long userId, BigDecimal chargeAmount) {
        PointsModel pointsModel = pointsRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 계정을 찾을 수 없습니다."));
        
        pointsModel.chargePoint(chargeAmount);
        return pointsRepository.save(pointsModel);
    }
    
    @Transactional
    public PointsModel deductPoints(Long userId, BigDecimal deductAmount) {
        // 비관적 잠금으로 조회하여 다른 트랜잭션의 접근 차단
        PointsModel pointsModel = pointsRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 계정을 찾을 수 없습니다."));
        
        pointsModel.deductPoint(deductAmount);
        return pointsRepository.save(pointsModel);
    }
    
    @Transactional(readOnly = true)
    public boolean hasEnoughPoints(Long userId, BigDecimal requiredAmount) {
        PointsModel pointsModel = pointsRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 계정을 찾을 수 없습니다."));
        
        return pointsModel.hasEnoughPoint(requiredAmount);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getPointBalance(Long userId) {
        PointsModel pointsModel = pointsRepository.findByUserId(userId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 계정을 찾을 수 없습니다."));
        
        return pointsModel.getPoint();
    }
    
    @Deprecated
    public BigDecimal chargePoints(PointsModel pointsModel, BigDecimal chargeAmount) {
        if (chargeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "0이하로 포인트를 충전할 수 없습니다.");
        }
        return pointsModel.getPoint().add(chargeAmount);
    }
    
    @Deprecated
    public BigDecimal deductPoints(PointsModel pointsModel, BigDecimal deductAmount) {
        if (deductAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "0 이하로 포인트를 차감할 수 없습니다.");
        }
        
        BigDecimal currentPoints = pointsModel.getPoint();
        if (currentPoints.compareTo(deductAmount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액이 부족합니다.");
        }
        
        return currentPoints.subtract(deductAmount);
    }
    
    @Deprecated
    public boolean hasEnoughPoints(PointsModel pointsModel, BigDecimal requiredAmount) {
        return pointsModel.getPoint().compareTo(requiredAmount) >= 0;
    }
}
