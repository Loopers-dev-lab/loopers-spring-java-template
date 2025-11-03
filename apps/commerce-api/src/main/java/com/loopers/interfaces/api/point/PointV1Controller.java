package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getPoint(
        @RequestHeader(value = "X-USER-ID") String userId
    ) {
        PointInfo info = pointFacade.getPoint(userId);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping("/charge")
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(
        @RequestHeader(value = "X-USER-ID") String userId,
        @Valid @RequestBody PointV1Dto.ChargeRequest request
    ) {
        PointInfo info = pointFacade.chargePoint(userId, request.amount());
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(info);
        return ApiResponse.success(response);
    }
}
