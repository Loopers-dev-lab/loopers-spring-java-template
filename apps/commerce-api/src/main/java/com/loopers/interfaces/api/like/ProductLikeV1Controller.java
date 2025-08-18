package com.loopers.interfaces.api.like;

import com.loopers.application.like.ProductLikeCommand;
import com.loopers.application.like.ProductLikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/like/products")
public class ProductLikeV1Controller implements ProductLikeV1ApiSpec {

    private final ProductLikeFacade productLikeFacade;

    public ProductLikeV1Controller(ProductLikeFacade productLikeFacade) {
        this.productLikeFacade = productLikeFacade;
    }

    @Override
    @PostMapping("/{productId}")
    public ApiResponse<ProductLikeV1Dto.LikeResponse> addProductLike(
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        
        productLikeFacade.addProductLikeIdempotent(userId, productId);
        
        return ApiResponse.success(
                ProductLikeV1Dto.LikeResponse.success(productId, true)
        );
    }

    @Override
    @DeleteMapping("/{productId}")
    public ApiResponse<ProductLikeV1Dto.LikeResponse> removeProductLike(
            @PathVariable Long productId,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        
        productLikeFacade.removeProductLikeIdempotent(userId, productId);
        
        return ApiResponse.success(
                ProductLikeV1Dto.LikeResponse.success(productId, false)
        );
    }

    @Override
    @GetMapping
    public ApiResponse<ProductLikeV1Dto.LikedProductsResponse> getLikedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        
        ProductLikeCommand.Request.GetLikedProducts request = 
                new ProductLikeCommand.Request.GetLikedProducts(userId, page, size);
        
        ProductLikeCommand.LikedProductsData result = productLikeFacade.getUserLikedProducts(request);
        
        return ApiResponse.success(
                ProductLikeV1Dto.LikedProductsResponse.of(
                        result.likedProductItems(),
                        result.productLikes().getTotalElements(),
                        result.productLikes().getNumber(),
                        result.productLikes().getSize()
                )
        );
    }
}