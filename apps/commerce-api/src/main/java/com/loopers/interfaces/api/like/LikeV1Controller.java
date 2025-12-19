package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/likes")
public class LikeV1Controller implements LikeV1ApiSpec {

  private final LikeFacade likeFacade;

  @PostMapping("/{productId}")
  @ResponseStatus(HttpStatus.CREATED)
  @Override
  public ApiResponse<LikeInfo> like(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @PathVariable(value = "productId") Long productId
  ) {
    LikeInfo info = likeFacade.like(userId, productId);
    return ApiResponse.success(info);
  }

  @DeleteMapping("/{productId}")
  @Override
  public ApiResponse<LikeInfo> unlike(
      @RequestHeader(value = "X-USER-ID", required = false) Long userId,
      @PathVariable(value = "productId") Long productId
  ) {
    LikeInfo info = likeFacade.unlike(userId, productId);
    return ApiResponse.success(info);
  }
}
