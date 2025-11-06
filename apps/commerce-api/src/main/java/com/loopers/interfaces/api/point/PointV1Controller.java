package com.loopers.interfaces.api.point;

import org.springframework.web.bind.annotation.*;

import com.loopers.application.point.PointFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@RestController
public class PointV1Controller {
    private final PointFacade pointFacade;

    @GetMapping(Uris.Point.GET_INFO)
    public ApiResponse<PointV1Dtos.PointInfoResponse> getPointInfo(@RequestHeader("X-USER-ID") String username) {
        PointV1Dtos.PointInfo pointInfo = pointFacade.getPointInfo(username);

        return ApiResponse.success(PointV1Dtos.PointInfoResponse.from(pointInfo));
    }

    @PostMapping(Uris.Point.CHARGE)
    public ApiResponse<PointV1Dtos.PointChargeResponse> chargePoint(
            @RequestHeader("X-USER-ID") String username,
            @Valid @RequestBody PointV1Dtos.PointChargeRequest request) {
        PointV1Dtos.PointChargeResponse response = pointFacade.chargePoint(username, request);

        return ApiResponse.success(response);
    }
}
