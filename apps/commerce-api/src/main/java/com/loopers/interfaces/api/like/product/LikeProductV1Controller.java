package com.loopers.interfaces.api.like.product;

import com.loopers.application.like.product.LikeProductFacade;
import com.loopers.application.like.product.LikeProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/like/products")
public class LikeProductV1Controller implements LikeProductV1ApiSpec {
    private final LikeProductFacade likeProductFacade;

    @RequestMapping(method = RequestMethod.POST, path = "/{productId}")
    @Override
    public ApiResponse<Void> likeProduct(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long productId) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        likeProductFacade.likeProduct(userId, productId);
        return ApiResponse.success(null);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/{productId}")
    @Override
    public ApiResponse<Void> unlikeProduct(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long productId
    ) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        likeProductFacade.unlikeProduct(userId, productId);
        return ApiResponse.success(null);
    }

    @RequestMapping(method = RequestMethod.GET)
    @Override
    public ApiResponse<LikeProductV1Dto.ProductsResponse> getLikedProducts(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (StringUtils.isBlank(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }
        Page<LikeProductInfo> likedProducts = likeProductFacade.getLikedProducts(userId, pageable);
        LikeProductV1Dto.ProductsResponse response = LikeProductV1Dto.ProductsResponse.from(likedProducts);
        return ApiResponse.success(response);
    }
}
