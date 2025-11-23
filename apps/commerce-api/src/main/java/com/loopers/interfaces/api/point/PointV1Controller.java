package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.point.Point;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getPoint(@RequestHeader("X-USER-ID") Long userId) {
        Point point = pointFacade.getPoint(userId);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(point.getBalance());
        return ApiResponse.success(response);
    }

    @PostMapping("/charge")
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(@RequestHeader("X-USER-ID") Long userId, @RequestBody PointV1Dto.PointChargeRequest request) {
        Money totalPoint = pointFacade.chargePoint(userId, request.toMoney());
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(totalPoint);
        return ApiResponse.success(response);
    }
}
