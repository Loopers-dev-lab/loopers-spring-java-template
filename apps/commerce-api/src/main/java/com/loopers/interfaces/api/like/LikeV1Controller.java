package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeService;
import com.loopers.domain.like.Like;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/likes")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeService likeService;

    @PostMapping("/users/{userId}/products/{productId}")
    @Override
    public ApiResponse<Void> like(
        @PathVariable String userId,
        @PathVariable Long productId
    ) {
        likeService.like(userId, productId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{userId}/products/{productId}")
    @Override
    public ApiResponse<Void> unlike(
        @PathVariable String userId,
        @PathVariable Long productId
    ) {
        likeService.unlike(userId, productId);
        return ApiResponse.success(null);
    }

    @GetMapping("/users/{userId}")
    @Override
    public ApiResponse<Page<LikeV1Dto.LikeResponse>> getLikesByUser(
        @PathVariable String userId,
        Pageable pageable
    ) {
        Page<Like> likes = likeService.getLikesByUser(userId, pageable);
        Page<LikeV1Dto.LikeResponse> response = likes.map(LikeV1Dto.LikeResponse::from);
        return ApiResponse.success(response);
    }

    @GetMapping("/products/{productId}/count")
    @Override
    public ApiResponse<LikeV1Dto.LikeCountResponse> getLikeCount(
        @PathVariable Long productId
    ) {
        Long likeCount = likeService.getLikeCount(productId);
        LikeV1Dto.LikeCountResponse response = LikeV1Dto.LikeCountResponse.of(productId,
            likeCount);
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}/products/{productId}")
    @Override
    public ApiResponse<LikeV1Dto.IsLikedResponse> isLiked(
        @PathVariable String userId,
        @PathVariable Long productId
    ) {
        boolean isLiked = likeService.isLiked(userId, productId);
        LikeV1Dto.IsLikedResponse response = LikeV1Dto.IsLikedResponse.of(userId, productId,
            isLiked);
        return ApiResponse.success(response);
    }
}
