package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetail;
import com.loopers.interfaces.api.support.PageInfo;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;

public class ProductDto {

  private ProductDto() {
  }

  public record ProductListResponse(
      List<ProductListItemResponse> products,
      PageInfo pageInfo
  ) {

    public static ProductListResponse from(Page<ProductDetail> page) {
      Objects.requireNonNull(page, "page는 null일 수 없습니다.");
      List<ProductListItemResponse> products = page.getContent().stream()
          .map(ProductListItemResponse::from)
          .toList();

      PageInfo pageInfo = PageInfo.from(page);

      return new ProductListResponse(products, pageInfo);
    }
  }

  public record ProductListItemResponse(
      Long productId,
      String name,
      Long price,
      BrandInfo brand,
      Long likeCount,
      boolean isLiked
  ) {

    public static ProductListItemResponse from(ProductDetail detail) {
      Objects.requireNonNull(detail, "detail은 null일 수 없습니다.");
      return new ProductListItemResponse(
          detail.productId(),
          detail.productName(),
          detail.price(),
          new BrandInfo(detail.brandId(), detail.brandName()),
          detail.likeCount(),
          detail.liked()
      );
    }
  }

  public record ProductResponse(
      Long productId,
      String name,
      Long price,
      String description,
      Long stock,
      BrandInfo brand,
      Long likeCount,
      boolean isLiked
  ) {

    public static ProductResponse from(ProductDetail detail) {
      Objects.requireNonNull(detail, "detail은 null일 수 없습니다.");
      return new ProductResponse(
          detail.productId(),
          detail.productName(),
          detail.price(),
          detail.description(),
          detail.stock(),
          new BrandInfo(detail.brandId(), detail.brandName()),
          detail.likeCount(),
          detail.liked()
      );
    }
  }

  public record BrandInfo(
      Long brandId,
      String name
  ) {

  }
}
