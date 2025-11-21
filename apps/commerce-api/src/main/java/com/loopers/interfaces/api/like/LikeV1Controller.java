package com.loopers.interfaces.api.like;

import org.springframework.web.bind.annotation.*;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.Uris;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping(Uris.Like.UPSERT)
    @Override
    public ApiResponse<LikeV1Dtos.LikeResponse> upsertLike(
            @RequestHeader("X-USER-ID") String username,
            @PathVariable Long productId
    ) {
        LikeInfo likeInfo = likeFacade.upsertLike(username, productId);
        LikeV1Dtos.LikeResponse response = LikeV1Dtos.LikeResponse.from(likeInfo);
        return ApiResponse.success(response);
    }

    @DeleteMapping(Uris.Like.CANCEL)
    @Override
    public ApiResponse<Void> unlikeProduct(
            @RequestHeader("X-USER-ID") String username,
            @PathVariable Long productId
    ) {
        likeFacade.unlikeProduct(username, productId);
        return ApiResponse.success(null);
    }
}

