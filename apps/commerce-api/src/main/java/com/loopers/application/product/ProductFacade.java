package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductDetail;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * packageName : com.loopers.application.product
 * fileName     : ProdcutFacade
 * author      : byeonsungmun
 * date        : 2025. 11. 10.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 10.     byeonsungmun       최초 생성
 */
@Component
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final ProductDomainService productDomainService;

    public Page<ProductInfo> getProducts(Long brandId, Pageable pageable, String sortBy) {

        SortType sortType = SortType.from(sortBy);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortType.toSort()
        );

        return productService.getProducts(brandId, sortedPageable)
                .map(product -> {
                    Brand brand = brandService.getBrand(product.getBrandId());
                    return ProductInfo.of(product, brand);
                });
    }

    public ProductDetailInfo getProduct(Long id) {
        ProductDetail productDetail = productDomainService.getProductDetail(id);
        return ProductDetailInfo.from(productDetail);
    }
}
