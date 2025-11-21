package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 좋아요 API v1 컨트롤러.
 * <p>
 * 상품 좋아요 추가, 삭제, 목록 조회 유즈케이스를 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/like/products")
public class LikeV1Controller {

    private final LikeFacade likeFacade;

    /**
     * 상품에 좋아요를 추가합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @param productId 상품 ID
     * @return 성공 응답
     */
    @PostMapping("/{productId}")
    public ApiResponse<Void> addLike(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable Long productId
    ) {
        likeFacade.addLike(userId, productId);
        return ApiResponse.success(null);
    }

    /**
     * 상품의 좋아요를 취소합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @param productId 상품 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeLike(
        @RequestHeader("X-USER-ID") String userId,
        @PathVariable Long productId
    ) {
        likeFacade.removeLike(userId, productId);
        return ApiResponse.success(null);
    }

    /**
     * 사용자가 좋아요한 상품 목록을 조회합니다.
     *
     * @param userId X-USER-ID 헤더로 전달된 사용자 ID
     * @return 좋아요한 상품 목록을 담은 API 응답
     */
    @GetMapping
    public ApiResponse<LikeV1Dto.LikedProductsResponse> getLikedProducts(
        @RequestHeader("X-USER-ID") String userId
    ) {
        var likedProducts = likeFacade.getLikedProducts(userId);
        return ApiResponse.success(LikeV1Dto.LikedProductsResponse.from(likedProducts));
    }
}

