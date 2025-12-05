package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.infrastructure.cache.product.ProductCacheService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductFacade {
    private final ProductService productService;
    private final ProductMetricsService productMetricsService;
    private final BrandService brandService;
    private final SupplyService supplyService;
    private final ProductCacheService productCacheService;

    @Transactional
    public ProductInfo createProduct(ProductCreateRequest request) {
        Brand brand = brandService.getBrandById(request.brandId());

        Product product = Product.create(request.name(), brand.getId(), request.price());
        product = productService.save(product);

        productMetricsService.save(ProductMetrics.create(product.getId(), brand.getId(), request.likeCount()));
        supplyService.save(Supply.create(product.getId(), request.stock()));

        // 상품 생성 시 해당 브랜드의 목록 캐시 무효화
        invalidateBrandListCache(brand.getId());

        return new ProductInfo(
                product.getId(),
                product.getName(),
                brand.getName(),
                product.getPrice().amount(),
                0,
                request.stock().quantity()
        );
    }

    @Transactional
    public List<ProductInfo> createProductBulk(List<ProductCreateRequest> requests) {
        List<Long> brandIds = requests.stream().map(ProductCreateRequest::brandId).distinct().toList();
        Map<Long, Brand> brandMap = brandService.getBrandMapByBrandIds(brandIds);
        if (brandMap.size() != brandIds.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "일부 브랜드를 찾을 수 없습니다.");
        }

        List<Product> products = productService.saveAll(requests.stream()
                .map(req -> Product.create(req.name(), req.brandId(), req.price()))
                .toList()
        );
        Map<ProductCreateRequest, Product> requestProductMap = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            requestProductMap.put(requests.get(i), products.get(i));
        }

        productMetricsService.saveAll(
                requestProductMap.entrySet().stream()
                        .map(entry -> {
                            Product product = entry.getValue();
                            Brand brand = brandMap.get(product.getBrandId());
                            return ProductMetrics.create(product.getId(), brand.getId(), entry.getKey().likeCount());
                        })
                        .toList()
        );
        supplyService.saveAll(
                requestProductMap.entrySet().stream()
                        .map(entry -> Supply.create(entry.getValue().getId(), entry.getKey().stock()))
                        .toList()
        );

        // 상품 일괄 생성 시 관련 브랜드의 목록 캐시 무효화
        brandIds.forEach(this::invalidateBrandListCache);

        return requestProductMap.entrySet().stream()
                .map(entry -> {
                    ProductCreateRequest req = entry.getKey();
                    Product product = entry.getValue();
                    Brand brand = brandMap.get(product.getBrandId());
                    return new ProductInfo(
                            product.getId(),
                            product.getName(),
                            brand.getName(),
                            product.getPrice().amount(),
                            req.likeCount(),
                            req.stock().quantity()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProductList(ProductSearchRequest request) {
        Pageable pageable = request.pageable();
        String sortStr = pageable.getSort().toString();
        List<Long> brandIds = request.filter().brandIds();
        boolean isSingleBrandFilter = brandIds.size() <= 1;
        Long singleBrandId = isSingleBrandFilter && !brandIds.isEmpty()
                ? brandIds.getFirst()
                : null;

        // 단일 브랜드 필터인 경우에만 캐시 조회
        if (isSingleBrandFilter) {
            Optional<Page<ProductInfo>> cacheResponse = productCacheService.getProductList(
                    singleBrandId, pageable.getPageNumber(), pageable.getPageSize(), sortStr);
            if (cacheResponse.isPresent()) {
                return cacheResponse.get();
            }
        }

        // 정렬 방식에 따라 분기
        boolean isLikeCountSort = sortStr.contains("likeCount: DESC");
        if (isLikeCountSort) {
            return getProductsByLikeCount(pageable, brandIds, singleBrandId);
        }

        return getProductsByDefaultSort(pageable, brandIds, singleBrandId);
    }

    private Page<ProductInfo> getProductsByLikeCount(Pageable pageable, List<Long> brandIds, Long singleBrandId) {
        Page<ProductMetrics> metricsPage = productMetricsService.getMetrics(brandIds, pageable);
        List<Long> productIds = metricsPage.map(ProductMetrics::getProductId).toList();

        Page<ProductInfo> result = mapMetricsToProductInfo(metricsPage, productIds);

        // 단일 브랜드 필터인 경우에만 캐시 저장
        if (brandIds.size() <= 1) {
            String sortStr = pageable.getSort().toString();
            productCacheService.setProductList(
                    singleBrandId, pageable.getPageNumber(), pageable.getPageSize(), sortStr, result);
        }

        return result;
    }

    private Page<ProductInfo> getProductsByDefaultSort(Pageable pageable, List<Long> brandIds, Long singleBrandId) {
        Page<Product> products = productService.getProductsByBrandIds(brandIds, pageable);
        List<Long> productIds = products.map(Product::getId).toList();

        Page<ProductInfo> result = mapProductsToProductInfo(products, productIds);

        // 단일 브랜드 필터인 경우에만 캐시 저장 (brandIds가 비어있으면 null로 처리하여 "all"로 캐싱)
        if (brandIds.size() <= 1) {
            String sortStr = pageable.getSort().toString();
            productCacheService.setProductList(singleBrandId, pageable.getPageNumber(), pageable.getPageSize(), sortStr, result);
        }

        return result;
    }

    private Page<ProductInfo> mapMetricsToProductInfo(Page<ProductMetrics> metricsPage, List<Long> productIds) {
        Map<Long, Product> productMap = productService.getProductMapByIds(productIds);
        Set<Long> brandIds = productMap.values().stream()
                .map(Product::getBrandId)
                .collect(Collectors.toSet());
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

    private Page<ProductInfo> mapProductsToProductInfo(Page<Product> products, List<Long> productIds) {
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

    @Transactional(readOnly = true)
    public ProductInfo getProductDetail(Long productId) {
        // 캐시 조회
        Optional<Optional<ProductInfo>> cacheResponse = productCacheService.getProductDetail(productId);
        if (cacheResponse.isPresent()) {
            Optional<ProductInfo> cachedResult = cacheResponse.get();
            return cachedResult.orElseThrow(
                    () -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
        }

        // DB 조회
        Optional<Product> productOpt = productService.getProductById(productId);
        if (productOpt.isEmpty()) {
            // 404 응답도 캐싱하여 DB 부하 감소
            productCacheService.setProductDetail(productId, Optional.empty());
            throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }

        Product product = productOpt.get();
        ProductMetrics metrics = productMetricsService.getMetricsByProductId(productId);
        Brand brand = brandService.getBrandById(product.getBrandId());
        Supply supply = supplyService.getSupplyByProductId(productId);

        ProductInfo productInfo = new ProductInfo(
                productId,
                product.getName(),
                brand.getName(),
                product.getPrice().amount(),
                metrics.getLikeCount(),
                supply.getStock().quantity()
        );

        // 캐시 저장
        productCacheService.setProductDetail(productId, Optional.of(productInfo));
        return productInfo;
    }

    private void invalidateBrandListCache(Long brandId) {
        try {
            productCacheService.invalidateProductList(brandId);
        } catch (Exception e) {
            // 캐시 무효화 실패해도 서비스는 정상 동작해야 함
            System.out.println("브랜드 목록 캐시 무효화 실패: " + e.getMessage());
        }
    }
}
