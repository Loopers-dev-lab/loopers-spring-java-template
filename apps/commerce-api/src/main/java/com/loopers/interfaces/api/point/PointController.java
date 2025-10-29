package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.point.PointDto.PointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController implements PointApiSpec {

  private static final String X_USER_ID = "X-USER-ID";
  private final PointFacade pointFacade;

  @Override
  @GetMapping
  public ApiResponse<PointResponse> getPoint(@RequestHeader(X_USER_ID) String userId) {
    PointResponse response = pointFacade.getPoint(userId)
        .map(PointResponse::from)
        .orElse(null);
    return ApiResponse.success(response);
  }
}
