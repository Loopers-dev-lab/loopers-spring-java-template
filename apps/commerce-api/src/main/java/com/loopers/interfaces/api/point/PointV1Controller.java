package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
@RestController
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getPoint(@PathVariable Long userId) {
        PointInfo pointInfo = pointFacade.getPoint(userId);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);

        return ApiResponse.success(response);
    }

    @PostMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(@RequestBody PointV1Dto.PointChargeRequest request) {
        PointInfo pointInfo = pointFacade.chargePoint(request);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);

        return ApiResponse.success(response);
    }
}
