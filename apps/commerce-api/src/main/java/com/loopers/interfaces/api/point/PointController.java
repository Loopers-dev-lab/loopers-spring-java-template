package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointController implements PointApiSpec {

    private final PointFacade  pointFacade;

    @GetMapping("/")
    @Override
    public ApiResponse<PointDto.PointResponse> getPoint(
            @RequestHeader(value = "X-USER-ID") String xUserId
    ) {
        PointInfo pointInfo = pointFacade.getPoint(xUserId);
        return ApiResponse.success(PointDto.PointResponse.from(pointInfo));
    }

    @PostMapping("/charge")
    @Override
    public ApiResponse<PointDto.PointResponse> charge(
            @RequestHeader(value = "X-USER-ID") String xUserId
            , @RequestBody PointDto.PointRequest pointRequest
    ) {
        PointInfo pointInfo = pointFacade.charge(xUserId, pointRequest.amount());
        return ApiResponse.success(PointDto.PointResponse.from(pointInfo));
    }
}
