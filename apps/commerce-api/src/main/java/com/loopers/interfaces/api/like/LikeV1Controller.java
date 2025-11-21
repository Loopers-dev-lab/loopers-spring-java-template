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

    /**
     * Create or update a user's like for a product and return its API representation.
     *
     * @param username  the user identifier from the `X-USER-ID` request header
     * @param productId the ID of the product to create or update the like for
     * @return an ApiResponse containing the created or updated LikeResponse
     */
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

    /**
     * Remove a user's like for the specified product.
     *
     * @param username  the user identifier from the `X-USER-ID` request header
     * @param productId the identifier of the product to unlike
     * @return an ApiResponse with a null payload indicating the operation succeeded
     */
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
