package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user/point")
public class PointV1Controller implements PointV1ApiSpec {

  private final PointFacade pointFacade;

  @GetMapping("")
  @Override
  public ApiResponse<BigDecimal> getPoint(@RequestHeader(value = "X-USER-ID", required = false) String userId
  ) {
    return ApiResponse.success(pointFacade.getPoint(userId));
  }
}
