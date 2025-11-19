package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetail;
import java.util.List;
import org.springframework.data.domain.Page;

public class ProductDto {

  public record ProductListResponse(
      List<ProductListItemResponse> products,
      PageInfo pageInfo
  ) {

    public static ProductListResponse from(Page<ProductDetail> page) {
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

  public record PageInfo(
      long totalElements,
      int currentPage,
      int totalPages,
      int pageSize,
      String sort
  ) {

    public static PageInfo from(Page<?> page) {
      String sortInfo = page.getSort().toString();

      return new PageInfo(
          page.getTotalElements(),
          page.getNumber(),
          page.getTotalPages(),
          page.getSize(),
          sortInfo
      );
    }
  }
}