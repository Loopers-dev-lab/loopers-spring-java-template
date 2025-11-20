package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductSortType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class ProductFacade {
    private final ProductService productService;
    private final BrandService brandService;

    public ProductWithBrandInfo getProductDetail(Long productId) {
        Product product = productService.getProduct(productId);
        Brand brand = brandService.getBrand(product.getBrandId());
        return ProductWithBrandInfo.from(product, brand);
    }

    public List<ProductWithBrandInfo> getProductList(ProductSortType sortType) {
        List<Product> products = productService.getProductList(sortType);

        Set<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .collect(Collectors.toSet());

        Map<Long, Brand> brandMap = getBrandMapByBrandIds(brandIds);

        return products.stream()
                .map(product -> ProductWithBrandInfo.from(product, brandMap.get(product.getBrandId())))
                .toList();
    }

    private Map<Long, Brand> getBrandMapByBrandIds(final Set<Long> brandIds) {
        List<Brand> listByBrandIds = brandService.getListByBrandIds(brandIds);
        return listByBrandIds.stream()
                .collect(Collectors.toMap(Brand::getId, b -> b));
    }

}
