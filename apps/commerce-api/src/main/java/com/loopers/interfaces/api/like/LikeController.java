package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/like")
public class LikeController implements LikeApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/products/{productId}")
    @Override
    public ApiResponse<Object> saveProductLike(
            @RequestHeader(value = "X-USER-ID") String xUserId
            , @PathVariable Long productId
    ) {
        likeFacade.saveProductLike(xUserId, productId);
        return ApiResponse.success();
    }

    @DeleteMapping("/products/{productId}")
    @Override
    public ApiResponse<Object> deleteProductLike(
            @RequestHeader(value = "X-USER-ID") String xUserId
            , @PathVariable Long productId
    ) {
        likeFacade.deleteProductLike(xUserId, productId);
        return ApiResponse.success();
    }
}

