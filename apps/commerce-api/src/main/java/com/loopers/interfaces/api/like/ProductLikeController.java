package com.loopers.interfaces.api.like;

import com.loopers.application.like.ProductLikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/like/products")
public class ProductLikeController implements ProductLikeApiSpec {

    private final ProductLikeFacade productLikeFacade;

    @Override
    @PostMapping("/{productId}")
    public ApiResponse<ProductLikeDto.LikeResponse> likeProduct(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long productId
    ) {
        ProductLikeDto.LikeResponse response = productLikeFacade.likeProduct(userId, productId);

        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/{productId}")
    public ApiResponse<ProductLikeDto.LikeResponse> unlikeProduct(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long productId
    ) {
        ProductLikeDto.LikeResponse response = productLikeFacade.unlikeProduct(userId, productId);

        return ApiResponse.success(response);
    }

    @Override
    @GetMapping
    public ApiResponse<ProductLikeDto.LikedProductsResponse> getLikedProducts(
            @RequestHeader("X-USER-ID") String userId
    ) {
        ProductLikeDto.LikedProductsResponse response = productLikeFacade.getLikedProducts(userId);

        return ApiResponse.success(response);
    }
}
