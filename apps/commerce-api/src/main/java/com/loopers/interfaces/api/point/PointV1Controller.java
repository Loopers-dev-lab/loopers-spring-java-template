package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.point.PointV1Dto.PointChargeRequest;
import com.loopers.interfaces.api.point.PointV1Dto.PointChargeResponse;
import com.loopers.interfaces.api.point.PointV1Dto.PointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
@RestController
public class PointV1Controller implements PointV1ApiSpec {
    private final PointFacade pointFacade;

    @Override
    @GetMapping("")
    public ApiResponse<PointResponse> getPoint(@RequestHeader(value = "X-USER-ID", required = false) final Long userModelId) {
        PointInfo pointInfo = pointFacade.getOrCreatePointByUserModelId(userModelId);
        return ApiResponse.success(PointV1Dto.PointResponse.from(pointInfo));
    }

    @Override
    @PostMapping("/charge")
    public ApiResponse<PointChargeResponse> chargePoint(
            @RequestHeader(value = "X-USER-ID", required = false) final Long userModelId,
            @RequestBody final PointChargeRequest pointChargeRequest
            ) {
        Long resultPoint = pointFacade.charge(new PointInfo(null, userModelId, pointChargeRequest.point()));
        return ApiResponse.success(PointV1Dto.PointChargeResponse.from(resultPoint));
    }
}
