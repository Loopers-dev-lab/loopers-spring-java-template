package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @Override
    @GetMapping
    public ApiResponse<PointV1Dto.PointResponse> getPoint(@RequestHeader("X-USER-ID") String userId) {
        PointInfo pointInfo = pointFacade.getPoint(userId);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/charge")
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(@RequestBody PointV1Dto.ChargePointRequest request) {
        PointInfo pointInfo = pointFacade.chargePoint(request);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);
        return ApiResponse.success(response);
    }
}
