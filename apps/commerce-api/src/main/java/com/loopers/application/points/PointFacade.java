package com.loopers.application.points;

import com.loopers.application.user.UserFacade;
import com.loopers.domain.points.PointsModel;
import com.loopers.domain.points.PointsRepository;
import com.loopers.domain.points.PointsService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointFacade {
    private final PointsService pointsService;
    private final UserFacade userFacade;
    private final PointsRepository pointsRepository;

    public PointFacade(PointsService pointsService, UserFacade userFacade, PointsRepository pointsRepository) {
        this.pointsService = pointsService;
        this.userFacade = userFacade;
        this.pointsRepository = pointsRepository;
    }
    public PointsCommand.PointInfo getPointInfo(Long userId) {
        if(!userFacade.isUserIdExists(userId)){
            return null;
        }
        PointsModel pointsModel = getOrCreatePointsByUserId(userId);
        return PointsCommand.PointInfo.from(pointsModel);
    }
    public PointsCommand.PointInfo chargePoints(Long userId, BigDecimal amount) {
        if(!userFacade.isUserIdExists(userId)){
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자를 찾을 수 없습니다.");
        }

        PointsModel result = getOrCreatePointsByUserId(userId);
        BigDecimal resultAmount = pointsService.chargePoints(result, amount);

        PointsModel pointsModel = PointsModel.from(result.getUserId(), resultAmount);

        return PointsCommand.PointInfo.from(pointsModel);
    }
    
    public PointsCommand.PointInfo deductPoints(Long userId, BigDecimal amount) {
        if(!userFacade.isUserIdExists(userId)){
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자를 찾을 수 없습니다.");
        }

        PointsModel pointsModel = getOrCreatePointsByUserId(userId);
        
        if (!pointsService.hasEnoughPoints(pointsModel, amount)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액이 부족합니다.");
        }
        
        BigDecimal resultAmount = pointsService.deductPoints(pointsModel, amount);
        PointsModel updatedPoints = PointsModel.from(pointsModel.getUserId(), resultAmount);
        
        save(updatedPoints);
        return PointsCommand.PointInfo.from(updatedPoints);
    }

    public PointsModel save(PointsModel user) {
        try {
            return pointsRepository.save(user);
        }catch (Exception e){
            throw new CoreException(ErrorType.INTERNAL_ERROR);
        }
    }
    @NonNull
    private PointsModel getOrCreatePointsByUserId(Long userId) {
        return pointsRepository.findByUserId(userId).orElseGet(
                () -> PointsModel.from(userId)
        );
    }
}
