package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.brand.Brands;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.Products;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.domain.productlike.ProductLikeStatuses;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductFacade {

  private final ProductService productService;
  private final BrandService brandService;
  private final ProductLikeService productLikeService;

  public Page<ProductDetail> searchProductDetails(Long brandId, Long userId, Pageable pageable) {
    Page<Product> productPage = productService.findProducts(brandId, pageable);

    Products products = Products.from(productPage.getContent());
    Brands brands = Brands.from(brandService.findByIdIn(products.getBrandIds()));
    ProductLikeStatuses likeStatuses = userId != null
        ? productLikeService.findLikeStatusByUser(userId, products.getProductIds())
        : ProductLikeStatuses.empty();

    Map<Long, ProductDetail> resultMap = products.toList().stream()
        .collect(Collectors.toMap(
            Product::getId,
            product -> ProductDetail.of(
                product,
                brands.getBrandById(product.getBrandId()),
                likeStatuses.isLiked(product.getId())
            )
        ));

    return productPage.map(product -> resultMap.get(product.getId()));
  }

  public ProductDetail viewProductDetail(Long productId, Long userId) {
    Product product = productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    Brand brand = brandService.getById(product.getBrandId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    boolean isLiked = userId != null && productLikeService.isLiked(userId, productId);

    return ProductDetail.of(product, brand, isLiked);
  }
}
