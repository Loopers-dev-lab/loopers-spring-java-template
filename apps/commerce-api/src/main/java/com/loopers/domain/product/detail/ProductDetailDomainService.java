package com.loopers.domain.product.detail;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.Brands;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Products;
import com.loopers.domain.productlike.ProductLikeStatuses;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductDetailDomainService {

  public ProductDetail get(Product product, Brand brand, boolean isLiked) {
    return ProductDetail.of(product, brand, isLiked);
  }

  public ProductDetails create(
      Products products,
      Brands brands,
      ProductLikeStatuses likeStatuses
  ) {
    List<ProductDetail> details = products.toList().stream()
        .map(product -> ProductDetail.of(
            product,
            brands.getBrandById(product.getBrandId()),
            likeStatuses.isLiked(product.getId())
        ))
        .toList();

    return ProductDetails.from(details);
  }
}
