package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductWithLikeCount;
import io.swagger.v3.oas.annotations.media.Schema;

public class ProductV1Dto {

  public record ProductCreateRequest(
      @Schema(description = "상품명", example = "아이폰 15 Pro")
      String name,

      @Schema(description = "브랜드 ID", example = "1")
      Long brandId,

      @Schema(description = "상품 가격", example = "1200000")
      Long price,

      @Schema(description = "통화 코드", example = "KRW")
      String currencyCode
  ) {
  }

  public record ProductResponse(
      @Schema(description = "상품 ID")
      Long id,

      @Schema(description = "상품명")
      String name,

      @Schema(description = "가격")
      Long price


  ) {
    public static ProductResponse from(ProductWithLikeCount info) {
      return new ProductResponse(
          info.id(),
          info.name(),
          info.price().longValue()
      );
    }
  }

  public record ProductDetailResponse(
      @Schema(description = "상품 ID")
      Long id,

      @Schema(description = "상품명")
      String name,

      @Schema(description = "브랜드명")
      String brandName,
      @Schema(description = "브랜드 스토리")
      String brandStory,
      @Schema(description = "가격")
      Long price,
      @Schema(description = "재고")
      Long stock,
      @Schema(description = "좋아요수")
      Long likeCount

  ) {
    public static ProductDetailResponse from(ProductDetailInfo info) {
      return new ProductDetailResponse(
          info.id(),
          info.name(),
          info.brandInfo().name(),
          info.brandInfo().story(),
          info.price().longValue(),
          info.stock(),
          info.likeInfo().likeCount()
      );
    }
  }

  public record ProductListResponse(
      @Schema(description = "상품 목록")
      java.util.List<ProductResponse> products,

      @Schema(description = "총 개수")
      int totalCount
  ) {
  }
}
