package com.loopers.application.product;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.event.ProductViewEvent;
import com.loopers.infrastructure.rank.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {

  private final ProductService productService;
  private final BrandService brandService;
  private final RankingService rankingService;
  private final ApplicationEventPublisher eventPublisher;

  public Page<ProductInfo> getProductsInfo(Pageable pageable) {
    Page<Product> products = productService.getProducts(pageable);
    return products.map(product -> {
      String brandName = brandService.getBrand(product.getBrandId())
          .getName();
      return ProductInfo.from(product, brandName, null);
    });
  }

  public ProductInfo getProductInfo(long id) {
    Product product = productService.getProduct(id);
    String brandName = brandService.getBrand(product.getBrandId())
        .getName();

    Integer currentRank = rankingService.getProductRank(id);

    eventPublisher.publishEvent(ProductViewEvent.from(id));

    return ProductInfo.from(product, brandName, currentRank);
  }

}
