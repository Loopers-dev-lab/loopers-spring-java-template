package com.loopers.application.product;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {
  private final ProductService productService;
  private final LikeService likeService;

  public Page<Product> getProductList(Long brandId,
                                      String sortType,
                                      int page,
                                      int size) {
    return productService.getProducts(brandId, sortType, page, size);
  }

  public ProductDetailInfo getProductDetail(long userId, long productId) {
    Product product = productService.getProduct(productId);
    boolean isLiked = likeService.isLiked(userId, productId);
    return ProductDetailInfo.from(product, isLiked);
  }

}
