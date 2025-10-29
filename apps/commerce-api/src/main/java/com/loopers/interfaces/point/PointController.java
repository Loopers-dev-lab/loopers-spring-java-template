package com.loopers.interfaces.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointResult;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointFacade pointFacade;

    @GetMapping("/{userId}")
    public ApiResponse<PointDto.Response> getPoint(@PathVariable String userId) {
        PointResult pointResult = pointFacade.getPoint(userId);
        PointDto.Response response = pointResult != null ? PointDto.Response.from(pointResult) : null;
        return ApiResponse.success(response);
    }
}
