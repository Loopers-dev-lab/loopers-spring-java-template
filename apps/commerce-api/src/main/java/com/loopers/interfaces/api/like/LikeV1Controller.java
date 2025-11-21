package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/products/{productId}/likes")
    public ApiResponse<Void> likeProduct(
            @PathVariable Long productId,
            @RequestHeader("X-USER-ID") String memberId
    ) {
        likeFacade.likeProduct(memberId, productId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/products/{productId}/likes")
    public ApiResponse<Void> unlikeProduct(
            @PathVariable Long productId,
            @RequestHeader("X-USER-ID") String memberId
    ) {
        likeFacade.unlikeProduct(memberId, productId);
        return ApiResponse.success(null);
    }
}