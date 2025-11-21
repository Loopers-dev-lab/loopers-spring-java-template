package com.loopers.interfaces.api.point;


import com.loopers.application.point.PointFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {
    private final PointFacade pointFacade;

    @RequestMapping(method = RequestMethod.GET)
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getUserPoints(@RequestHeader(value = "X-USER-ID", required = false) String userId) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        Long currentPoint = pointFacade.getCurrentPoint(userId);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(currentPoint);
        return ApiResponse.success(response);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/charge")
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargeUserPoints(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestBody PointV1Dto.PointChargeRequest request) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        Long chargedPoint = pointFacade.chargePoint(userId, request.amount());
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(chargedPoint);
        return ApiResponse.success(response);
    }
}
