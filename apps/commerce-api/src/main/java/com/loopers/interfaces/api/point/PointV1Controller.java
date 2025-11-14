package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user/point")
public class PointV1Controller implements PointV1ApiSpec {

  private final PointFacade pointFacade;

  @GetMapping("")
  @Override
  public ApiResponse<BigDecimal> getPoint(@RequestHeader(value = "X-USER-ID", required = false) Long userId
  ) {
    return ApiResponse.success(pointFacade.getPoint(userId));
  }

  @PostMapping("/charge")
  @Override
  public ApiResponse<BigDecimal> charge(@RequestHeader(value = "X-USER-ID", required = false) Long userId
      , @RequestBody BigDecimal amount
  ) {
    return ApiResponse.success(pointFacade.charge(userId, amount));
  }
}
