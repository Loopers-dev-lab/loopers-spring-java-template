package com.loopers.interfaces.api.pointwallet;

import com.loopers.application.pointwallet.PointWalletFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 포인트 관리 API v1 컨트롤러.
 * <p>
 * 사용자의 포인트 조회 및 충전 유즈케이스를 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class PointWalletV1Controller {

    private final PointWalletFacade pointWalletFacade;

    /**
     * 현재 사용자의 포인트를 조회합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @return 포인트 정보를 담은 API 응답
     * @throws CoreException 사용자를 찾을 수 없는 경우
     */
    @GetMapping("/me/points")
    public ApiResponse<PointWalletV1Dto.PointsResponse> getMyPoints(
        @RequestHeader("X-USER-ID") String userId
    ) {
        PointWalletFacade.PointsInfo pointsInfo = pointWalletFacade.getPoints(userId);
        return ApiResponse.success(PointWalletV1Dto.PointsResponse.from(pointsInfo));
    }

    /**
     * 현재 사용자의 포인트를 충전합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @param request 충전 요청 데이터 (amount)
     * @return 충전된 포인트 정보를 담은 API 응답
     * @throws CoreException 사용자를 찾을 수 없거나 충전 금액이 유효하지 않은 경우
     */
    @PostMapping("/me/points/charge")
    public ApiResponse<PointWalletV1Dto.PointsResponse> chargePoints(
        @RequestHeader("X-USER-ID") String userId,
        @Valid @RequestBody PointWalletV1Dto.ChargeRequest request
    ) {
        PointWalletFacade.PointsInfo pointsInfo = pointWalletFacade.chargePoint(userId, request.amount());
        return ApiResponse.success(PointWalletV1Dto.PointsResponse.from(pointsInfo));
    }
}

