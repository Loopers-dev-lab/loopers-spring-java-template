package com.loopers.application.product;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductFacade {
  private final ProductService productService;
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
    Product product = productService.getProduct(productId);
    boolean isLiked = likeService.isLiked(userId, productId);
    return ProductDetailInfo.from(product, isLiked);
  }

}
