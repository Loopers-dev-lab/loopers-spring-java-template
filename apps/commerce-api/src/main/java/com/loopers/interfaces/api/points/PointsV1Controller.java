package com.loopers.interfaces.api.points;

import com.loopers.application.points.PointsCommand;
import com.loopers.application.points.PointsFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/points")
public class PointsV1Controller implements PointsV1ApiSpec {
    private final PointsFacade pointsFacade;

    public PointsV1Controller(PointsFacade pointsFacade) {
        this.pointsFacade = pointsFacade;
    }

    @Override
    @GetMapping
    public ApiResponse<PointsV1Dto.PointsResponse> getPoints(
            @RequestHeader("X-USER-ID") Long userId) {
        PointsCommand.PointInfo pointInfo = pointsFacade.getPointInfo(userId);
        if (pointInfo == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        return ApiResponse.success(
                PointsV1Dto.PointsResponse.from(pointInfo)
        );

    }
    @Override
    @PostMapping("/charge")
    public ApiResponse<PointsV1Dto.PointsResponse> chargePoints(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody PointsV1Dto.PointsRequest pointsV1Request) {
        BigDecimal amount = pointsV1Request.amount();
        return ApiResponse.success(
                PointsV1Dto.PointsResponse.from(
                        pointsFacade.chargePoints(userId, amount)
                )
        );
    }
}
