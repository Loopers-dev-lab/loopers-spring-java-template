package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.brand.Brands;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.detail.ProductDetail;
import com.loopers.domain.product.detail.ProductDetailDomainService;
import com.loopers.domain.product.detail.ProductDetails;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.Products;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.domain.productlike.ProductLikeStatuses;
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
  private final ProductDetailDomainService productDetailDomainService;

  public Page<ProductListResponse> getProducts(Long brandId, Long userId, Pageable pageable) {
    Page<Product> productPage = brandId != null
        ? productService.findByBrandId(brandId, pageable)
        : productService.findAll(pageable);

    Products products = Products.from(productPage.getContent());
    Brands brands = Brands.from(brandService.findByIdIn(products.getBrandIds()));
    ProductLikeStatuses likeStatuses = userId != null
        ? productLikeService.findLikeStatusByUser(userId, products.getProductIds())
        : ProductLikeStatuses.empty();

    ProductDetails productDetails = productDetailDomainService.create(products, brands, likeStatuses);

    return productPage.map(product ->
        ProductListResponse.from(productDetails.get(product.getId()))
    );
  }

  public ProductDetailResponse getProduct(Long productId, Long userId) {
    Product product = productService.getById(productId);
    Brand brand = brandService.getById(product.getBrandId());
    boolean isLiked = productLikeService.isLiked(userId, productId);

    ProductDetail productDetail = productDetailDomainService.get(product, brand, isLiked);

    return ProductDetailResponse.from(productDetail);
  }
}
