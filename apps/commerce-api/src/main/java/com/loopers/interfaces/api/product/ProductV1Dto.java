package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductListItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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

  public record ProductListsResponse(
      @Schema(description = "상품 목록") List<ProductListResponse> products,

      @Schema(description = "총 개수")
      Long totalCount
  ) {
  }

  public record ProductListResponse(
      @Schema(description = "상품 ID")
      Long id,

      @Schema(description = "상품명")
      String name,

      @Schema(description = "가격")
      Long price,

      @Schema(description = "랭킹 순위")
      Integer rank

  ) {
    public static ProductListResponse from(ProductListItem info) {
      return new ProductListResponse(
          info.id(),
          info.name(),
          info.price().longValue(),
          info.rank()
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
      Long likeCount,

      @Schema(description = "랭킹 순위")
      Integer rank

  ) {
    public static ProductDetailResponse from(ProductDetailInfo info) {
      return new ProductDetailResponse(
          info.id(),
          info.name(),
          info.brandInfo().name(),
          info.brandInfo().story(),
          info.price().longValue(),
          info.stock(),
          info.likeInfo().likeCount(),
          info.rank() != null ? info.rank() : null
      );
    }
  }


}
