package com.loopers.interfaces.api.like;

import com.loopers.domain.productlike.LikeSortType;
import com.loopers.domain.productlike.LikedProduct;
import com.loopers.interfaces.api.support.PageInfo;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;

public class LikeDto {

  private LikeDto() {
  }

  public record LikedProductListResponse(
      List<LikedProductResponse> products,
      PageInfo pageInfo
  ) {

    public static LikedProductListResponse from(Page<LikedProduct> page) {
      Objects.requireNonNull(page, "page는 null일 수 없습니다.");
      List<LikedProductResponse> products = page.getContent().stream()
          .map(LikedProductResponse::from)
          .toList();

      return new LikedProductListResponse(products, PageInfo.from(page));
    }

    public static LikedProductListResponse from(Page<LikedProduct> page, LikeSortType sortType) {
      Objects.requireNonNull(page, "page는 null일 수 없습니다.");
      Objects.requireNonNull(sortType, "sortType은 null일 수 없습니다.");
      List<LikedProductResponse> products = page.getContent().stream()
          .map(LikedProductResponse::from)
          .toList();

      return new LikedProductListResponse(products, PageInfo.from(page, sortType.sortDescription()));
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
      Objects.requireNonNull(likedProduct, "likedProduct는 null일 수 없습니다.");
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
}
