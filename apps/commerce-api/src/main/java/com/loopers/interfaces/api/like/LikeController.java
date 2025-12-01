package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.productlike.LikeSortType;
import com.loopers.domain.productlike.LikedProduct;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.like.LikeDto.LikedProductListResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@RequiredArgsConstructor
public class LikeController implements LikeApiSpec {

  private final LikeFacade likeFacade;

  @Override
  @PostMapping("/{productId}")
  public ApiResponse<Object> registerProductLike(
      @RequestHeader(ApiHeaders.USER_ID) Long userId,
      @PathVariable("productId") Long productId
  ) {
    likeFacade.registerProductLike(userId, productId);
    return ApiResponse.success();
  }

  @Override
  @DeleteMapping("/{productId}")
  public ApiResponse<Object> cancelProductLike(
      @RequestHeader(ApiHeaders.USER_ID) Long userId,
      @PathVariable("productId") Long productId
  ) {
    likeFacade.cancelProductLike(userId, productId);
    return ApiResponse.success();
  }

  @Override
  @GetMapping
  public ApiResponse<LikedProductListResponse> retrieveLikedProducts(
      @RequestHeader(ApiHeaders.USER_ID) Long userId,
      @RequestParam(defaultValue = "latest") LikeSortType sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    Page<LikedProduct> likedProducts = likeFacade.retrieveLikedProducts(userId, sort, pageable);
    LikedProductListResponse response = LikedProductListResponse.from(likedProducts, sort);
    return ApiResponse.success(response);
  }
}
