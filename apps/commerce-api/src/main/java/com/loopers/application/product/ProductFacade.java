package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductFacade {
    private final ProductService productService;
    private final ProductMetricsService productMetricsService;
    private final BrandService brandService;
    private final SupplyService supplyService;

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProductList(Pageable pageable) {
        String sortStr = pageable.getSort().toString().split(":")[0];
        if (StringUtils.equals(sortStr, "like_desc")) {
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            Sort  sort = Sort.by(Sort.Direction.DESC, "likeCount");
            return getProductsByLikeCount(PageRequest.of(page, size, sort));
        }

        Page<Product> products = productService.getProducts(pageable);

        List<Long> productIds = products.map(Product::getId).toList();
        Set<Long> brandIds = products.map(Product::getBrandId).toSet();

        Map<Long, ProductMetrics> metricsMap = productMetricsService.getMetricsMapByProductIds(productIds);
        Map<Long, Supply> supplyMap = supplyService.getSupplyMapByProductIds(productIds);
        Map<Long, Brand> brandMap = brandService.getBrandMapByBrandIds(brandIds);

        return products.map(product -> {
            ProductMetrics metrics = metricsMap.get(product.getId());
            if (metrics == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 메트릭 정보를 찾을 수 없습니다.");
            }
            Brand brand = brandMap.get(product.getBrandId());
            if (brand == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 브랜드 정보를 찾을 수 없습니다.");
            }
            Supply supply = supplyMap.get(product.getId());
            if (supply == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 재고 정보를 찾을 수 없습니다.");
            }

            return new ProductInfo(
                    product.getId(),
                    product.getName(),
                    brand.getName(),
                    product.getPrice().amount(),
                    metrics.getLikeCount(),
                    supply.getStock().quantity()
            );
        });
    }

    public Page<ProductInfo> getProductsByLikeCount(Pageable pageable) {
        Page<ProductMetrics> metricsPage = productMetricsService.getMetrics(pageable);
        List<Long> productIds = metricsPage.map(ProductMetrics::getProductId).toList();
        Map<Long, Product> productMap = productService.getProductMapByIds(productIds);
        Set<Long> brandIds = productMap.values().stream().map(Product::getBrandId).collect(Collectors.toSet());
        Map<Long, Brand> brandMap = brandService.getBrandMapByBrandIds(brandIds);
        Map<Long, Supply> supplyMap = supplyService.getSupplyMapByProductIds(productIds);

        return metricsPage.map(metrics -> {
            Product product = productMap.get(metrics.getProductId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "해당 상품 정보를 찾을 수 없습니다.");
            }
            Brand brand = brandMap.get(product.getBrandId());
            if (brand == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 브랜드 정보를 찾을 수 없습니다.");
            }
            Supply supply = supplyMap.get(product.getId());
            if (supply == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "해당 상품의 재고 정보를 찾을 수 없습니다.");
            }

            return new ProductInfo(
                    product.getId(),
                    product.getName(),
                    brand.getName(),
                    product.getPrice().amount(),
                    metrics.getLikeCount(),
                    supply.getStock().quantity()
            );
        });
    }

    @Transactional(readOnly = true)
    public ProductInfo getProductDetail(Long productId) {
        Product product = productService.getProductById(productId);
        ProductMetrics metrics = productMetricsService.getMetricsByProductId(productId);
        Brand brand = brandService.getBrandById(product.getBrandId());
        Supply supply = supplyService.getSupplyByProductId(productId);

        return new ProductInfo(
                productId,
                product.getName(),
                brand.getName(),
                product.getPrice().amount(),
                metrics.getLikeCount(),
                supply.getStock().quantity()
        );
    }
}
