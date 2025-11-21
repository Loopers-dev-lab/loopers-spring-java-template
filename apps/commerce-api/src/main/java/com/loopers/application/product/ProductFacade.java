package com.loopers.application.product;

import com.loopers.application.like.LikeInfo;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.ProductIdAndLikeCount;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductFacade {
  private final ProductService productService;
  private final StockService stockService;
  private final LikeService likeService;

  @Transactional(readOnly = true)
  public Page<ProductWithLikeCount> getProductList(Long brandId,
                                      String sortType,
                                      int page,
                                      int size) {
    Page<Product> productPage = productService.getProducts(brandId, sortType, page, size);
    List<Product> products = productPage.getContent();

    List<Long> productIds = products.stream()
        .map(Product::getId)
        .toList();

    List<ProductIdAndLikeCount> likeCountList = likeService.getLikeCount(productIds);

    // 총 좋아요 정보
    Map<Long, Long> likeCountMap = likeCountList.stream()
        .collect(Collectors.toMap(
            ProductIdAndLikeCount::getProductId,
            ProductIdAndLikeCount::getLikeCount
        ));

    //

    //
    List<ProductWithLikeCount> dtoList = products.stream()
        .map(product -> {
          Long productId = product.getId();
          Long totalLikeCount = likeCountMap.getOrDefault(productId, 0L);

          return new ProductWithLikeCount(
              productId,
              product.getName(),
              product.getPrice().getAmount(),
              totalLikeCount
          );
        })
        .toList();

    return new PageImpl<>(dtoList, productPage.getPageable(), productPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public ProductDetailInfo getProductDetail(long userId, long productId) {
    Product product = productService.getExistingProduct(productId);
    Stock stock = stockService.findByProductId(productId);
    long likeCount = likeService.getLikeCount(productId);
    boolean isLiked = likeService.isLiked(userId, productId);

    LikeInfo likeInfo = LikeInfo.from(likeCount, isLiked);
    return ProductDetailInfo.from(product, stock, likeInfo);
  }

}
