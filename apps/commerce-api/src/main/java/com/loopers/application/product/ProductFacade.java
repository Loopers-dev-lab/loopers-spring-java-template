package com.loopers.application.product;

import com.loopers.application.like.LikeInfo;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductFacade {
  private final ProductService productService;
  private final StockService stockService;
  private final LikeService likeService;

  @Transactional(readOnly = true)
  public Page<Product> getProductList(Long brandId,
                                      String sortType,
                                      int page,
                                      int size) {
    return productService.getProducts(brandId, sortType, page, size);
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
