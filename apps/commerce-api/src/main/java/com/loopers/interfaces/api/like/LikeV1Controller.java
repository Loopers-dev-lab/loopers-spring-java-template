package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/like/products")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/{productId}")
    @Override
    public ApiResponse<Object> addLike(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long productId
    ) {
        likeFacade.addLike(userId, productId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{productId}")
    @Override
    public ApiResponse<Object> removeLike(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long productId
    ) {
        likeFacade.removeLike(userId, productId);
        return ApiResponse.success();
    }

    @GetMapping
    @Override
    public ApiResponse<List<LikeV1Dto.LikedProductResponse>> getLikedProducts(
        @RequestHeader("X-USER-ID") Long userId
    ) {
        List<LikeInfo> infos = likeFacade.getLikedProducts(userId);

        List<LikeV1Dto.LikedProductResponse> responses =
                infos.stream()
                .map(LikeV1Dto.LikedProductResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
