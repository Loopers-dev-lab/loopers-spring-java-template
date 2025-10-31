package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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

    /**
     * Retrieve the point balance for the user identified by the X-USER-ID request header.
     *
     * @param userId the value of the `X-USER-ID` request header; must not be null or empty (after trimming)
     * @return an ApiResponse wrapping a PointResponse containing the user's point balance; if no point record exists, the response contains a balance of 0
     * @throws CoreException if `userId` is null or empty (ErrorType.BAD_REQUEST)
     */
    @GetMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> getPoint(
            @RequestHeader(value = "X-USER-ID") String userId
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-USER-ID'는 빈 값일 수 없습니다.");
        }

        PointInfo pointInfo = pointFacade.getPointByUserId(userId);
        if (pointInfo == null) {
            // 포인트가 없으면 0으로 초기화하여 반환
            return ApiResponse.success(new PointV1Dto.PointResponse(0L));
        }

        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);
        return ApiResponse.success(response);
    }

    /**
     * Charge points for the user identified by the X-USER-ID header and return the updated point balance.
     *
     * @param userId the user identifier taken from the `X-USER-ID` request header
     * @param request the charge request containing the amount to add; amount must be greater than 0
     * @return an ApiResponse wrapping a PointResponse with the user's updated point total
     * @throws CoreException if the `X-USER-ID` header is missing or empty, or if the request is null or contains a non-positive amount
     */
    @PostMapping("/charge")
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @RequestHeader(value = "X-USER-ID") String userId,
            @RequestBody PointV1Dto.ChargeRequest request
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-USER-ID'는 빈 값일 수 없습니다.");
        }
        if (request == null || request.amount() == null || request.amount() <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
        }
        PointInfo charged = pointFacade.charge(userId, request.amount());
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(charged);
        return ApiResponse.success(response);
    }
}