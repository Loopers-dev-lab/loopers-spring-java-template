package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointResult;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.point.PointDto.ChargeRequest;
import com.loopers.interfaces.api.point.PointDto.ChargeResponse;
import com.loopers.interfaces.api.point.PointDto.PointResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController implements PointApiSpec {

  private final PointFacade pointFacade;

  @Override
  @GetMapping
public ApiResponse<PointResponse> getPoint(@RequestHeader(ApiHeaders.USER_ID) String userId) {
    PointResult result = pointFacade.findPointOrNull(userId);
    PointResponse response = result == null ? null : PointResponse.from(result);
    return ApiResponse.success(response);
  }

  @Override
  @PatchMapping("/charge")
  public ApiResponse<ChargeResponse> chargePoint(
      @RequestHeader(ApiHeaders.USER_ID) String userId,
      @Valid @RequestBody ChargeRequest request
  ) {
    ChargeResponse response = ChargeResponse.from(
        pointFacade.charge(userId, request.amount())
    );
    return ApiResponse.success(response);
  }
}
