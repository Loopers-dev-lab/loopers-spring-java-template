package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/points")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<PointV1Dto.PointBalanceResponse> getPointBalance(
            @PathVariable("userId") String userId
    ) {
        PointInfo info = pointFacade.getBalance(userId);
        PointV1Dto.PointBalanceResponse response = PointV1Dto.PointBalanceResponse.from(info);
        return ApiResponse.success(response);
    }
}
