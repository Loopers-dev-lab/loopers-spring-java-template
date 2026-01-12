package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.user.UserId;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/like")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/products/{productId}")
    @Override
    public ApiResponse<Void> addLike(
        @RequestHeader(value = "X-USER-ID") UserId userId,
        @PathVariable("productId") Long productId
    ) {
        likeFacade.addLike(userId, productId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/products/{productId}")
    @Override
    public ApiResponse<Void> removeLike(
        @RequestHeader(value = "X-USER-ID") @NotBlank(message = "X-USER-ID는 필수입니다.") UserId userId,
        @PathVariable("productId") Long productId
    ) {
        likeFacade.removeLike(userId, productId);
        return ApiResponse.success(null);
    }

    @GetMapping("/products")
    @Override
    public ApiResponse<LikeV1Dto.LikedProductsResponse> getLikedProducts(
        @RequestHeader(value = "X-USER-ID") @NotBlank(message = "X-USER-ID는 필수입니다.") UserId userId
    ) {
        List<ProductModel> products = likeFacade.getLikedProducts(userId);
        LikeV1Dto.LikedProductsResponse response = LikeV1Dto.LikedProductsResponse.from(products);
        return ApiResponse.success(response);
    }
}

