package com.loopers.interfaces.api.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/like/products")
public class LikeV1Controller {
    private final LikeService likeService;

    @PostMapping("/{productId}")
    public ApiResponse<Object> likeProduct(
        @RequestHeader(value = "X-USER-ID") String userId,
        @PathVariable("productId") Long productId
    ) {
        likeService.likeProduct(userId, productId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Object> unlikeProduct(
        @RequestHeader(value = "X-USER-ID") String userId,
        @PathVariable("productId") Long productId
    ) {
        likeService.cancleLikeProduct(userId, productId);
        return ApiResponse.success();
    }

    @GetMapping("")
    public ApiResponse<LikeV1Dto.LikedProductsResponse> getLikedProducts(
        @RequestHeader(value = "X-USER-ID") String userId
    ) {
        List<Like> likes = likeService.getUserLikeProduct(userId);
        List<Long> productIds = likes.stream().map(Like::getProductId).collect(Collectors.toList());
        return ApiResponse.success(new LikeV1Dto.LikedProductsResponse(productIds));
    }
}
