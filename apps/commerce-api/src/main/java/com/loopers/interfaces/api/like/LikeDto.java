package com.loopers.interfaces.api.like;

import com.loopers.domain.productlike.LikedProduct;
import java.util.List;
import org.springframework.data.domain.Page;

public class LikeDto {

  private LikeDto() {
  }

  public record LikedProductListResponse(
      List<LikedProductResponse> products,
      PageInfo pageInfo
  ) {

    public static LikedProductListResponse from(Page<LikedProduct> page) {
      List<LikedProductResponse> products = page.getContent().stream()
          .map(LikedProductResponse::from)
          .toList();

      return new LikedProductListResponse(products, PageInfo.from(page));
    }
  }

  public record LikedProductResponse(
      Long productId,
      String productName,
      Long price,
      Long likeCount,
      BrandInfo brand
  ) {

    public static LikedProductResponse from(LikedProduct likedProduct) {
      return new LikedProductResponse(
          likedProduct.productId(),
          likedProduct.productName(),
          likedProduct.price(),
          likedProduct.likeCount(),
          new BrandInfo(likedProduct.brandId(), likedProduct.brandName())
      );
    }
  }

  public record BrandInfo(
      Long brandId,
      String name
  ) {
  }

  public record PageInfo(
      long totalElements,
      int currentPage,
      int totalPages,
      int pageSize,
      String sort
  ) {

    public static PageInfo from(Page<?> page) {
      return new PageInfo(
          page.getTotalElements(),
          page.getNumber(),
          page.getTotalPages(),
          page.getSize(),
          page.getSort().toString()
      );
    }
  }
}
