package com.loopers.interfaces.api.points;

import com.loopers.domain.points.PointService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointService pointService;

    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getMemberPoints(
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.MISSING_REQUEST_HEADER, "X-USER-ID 헤더는 필수입니다.");
        }
        
        BigDecimal points = pointService.getMemberPoints(userId);
        PointV1Dto.PointResponse response = new PointV1Dto.PointResponse(points);
        return ApiResponse.success(response);
    }
}