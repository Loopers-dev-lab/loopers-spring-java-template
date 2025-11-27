package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class ProductFacade {
    private static final Duration TTL = Duration.ofMinutes(1);

    private final ProductCacheService productCacheService;
    private final ProductService productService;
    private final BrandService brandService;

    public ProductWithBrandInfo getProductDetail(final Long productId) {
        ProductWithBrandInfo cached = productCacheService.readDetail(productId);
        if (!Objects.isNull(cached)) {
            return cached;
        }

        Product product = productService.getProduct(productId);
        Brand brand = brandService.getBrand(product.getBrandId());
        ProductWithBrandInfo result = ProductWithBrandInfo.from(product, brand);

        productCacheService.createOrUpdateDetail(productId, result, TTL);

        return result;
    }

    public List<ProductWithBrandInfo> getProductList(final ProductSearchCriteria productSearchCriteria) {
        List<ProductWithBrandInfo> cached = productCacheService.readList(productSearchCriteria);
        if (!Objects.isNull(cached)) {
            return cached;
        }

        List<Product> products = productService.getProductList(productSearchCriteria.brandId(),
                productSearchCriteria.productSortType(),
                PageRequest.of(productSearchCriteria.page() - 1, productSearchCriteria.size())
                );

        Set<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .collect(Collectors.toSet());

        Map<Long, Brand> brandMap = getBrandMapByBrandIds(brandIds);
        List<ProductWithBrandInfo> result = products.stream()
                .map(product -> ProductWithBrandInfo.from(product, brandMap.get(product.getBrandId())))
                .toList();
        productCacheService.createOrUpdateList(productSearchCriteria, result, TTL);

        return result;
    }

    private Map<Long, Brand> getBrandMapByBrandIds(final Set<Long> brandIds) {
        List<Brand> listByBrandIds = brandService.getListByBrandIds(brandIds);
        return listByBrandIds.stream()
                .collect(Collectors.toMap(Brand::getId, b -> b));
    }

}
