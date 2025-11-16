package com.loopers.application.api.productlike;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.service.productlike.ProductLikeService;
import com.loopers.core.service.productlike.command.ProductLikeCommand;
import com.loopers.core.service.productlike.command.ProductUnlikeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/like/products")
public class ProductLikeV1Api implements ProductLikeV1ApiSpec {

    private final ProductLikeService productLikeService;

    @Override
    @PostMapping("/{productId}")
    public ApiResponse<Void> likeProduct(
            @PathVariable String productId,
            @RequestHeader(name = "X-USER-ID") String userIdentifier
    ) {
        productLikeService.like(new ProductLikeCommand(userIdentifier, productId));
        return ApiResponse.success(null);
    }

    @Override
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> unlikeProduct(
            @PathVariable String productId,
            @RequestHeader(name = "X-USER-ID") String userIdentifier
    ) {
        productLikeService.unlike(new ProductUnlikeCommand(userIdentifier, productId));
        return ApiResponse.success(null);
    }
}
