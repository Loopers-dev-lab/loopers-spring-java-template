package com.loopers.interfaces.api.like;

import com.loopers.application.like.ProductLikeFacade;
import com.loopers.application.like.ProductLikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/product-likes")
public class ProductLikeV1Controller implements ProductLikeV1ApiSpec{

    private final ProductLikeFacade productLikeFacade;

    @Override
    @PostMapping("/new")
    public ApiResponse<ProductLikeV1Dto.ProductLikeResponse> addProductLike(
                @RequestBody Long productId,
                @RequestHeader(value = "X-USER-ID") String headerUserId
    ) {
        ProductLikeInfo productLikeInfo = productLikeFacade.addLike(productId, headerUserId);

        ProductLikeV1Dto.ProductLikeResponse response =
                ProductLikeV1Dto.ProductLikeResponse.from(
                            productLikeInfo.id(),
                            productLikeInfo.productIdx(),
                            productLikeInfo.likeUserIdx()
                        );

        return ApiResponse.success(response);
    }
}
