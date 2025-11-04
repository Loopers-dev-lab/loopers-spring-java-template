package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/point")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @PostMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @RequestHeader("X-USER-ID") String loginId,
            @Valid @RequestBody PointV1Dto.PointRequest pointRequest
    ) {
        PointCommand.ChargeCommand chargeCommand = pointRequest.toCommand(loginId);

        PointInfo pointInfo = pointFacade.chargePoint(chargeCommand);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);

        return ApiResponse.success(response);
    }

    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getPointInfo(@RequestHeader(value = "X-USER-ID", required = false) String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-USER-ID 헤더는 필수입니다.");
        }

        PointInfo pointInfo = pointFacade.getPointInfo(loginId);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);

        return ApiResponse.success(response);
    }

}
