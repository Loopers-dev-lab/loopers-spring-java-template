package com.loopers.application.like.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.product.LikeProduct;
import com.loopers.domain.like.product.LikeProductService;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LikeProductFacade {
    private final LikeProductService likeProductService;
    private final UserService userService;
    private final ProductService productService;
    private final ProductMetricsService productMetricsService;
    private final BrandService brandService;
    private final SupplyService supplyService;

    public void likeProduct(String userId, Long productId) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (!productService.existsById(productId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        likeProductService.likeProduct(user.getId(), productId);
    }

    public void unlikeProduct(String userId, Long productId) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (!productService.existsById(productId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        likeProductService.unlikeProduct(user.getId(), productId);
    }

    public Page<LikeProductInfo> getLikedProducts(String userId, Pageable pageable) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Page<LikeProduct> likedProducts = likeProductService.getLikedProducts(user.getId(), pageable);

        List<Long> productIds = likedProducts.map(LikeProduct::getProductId).toList();
        Map<Long, Product> productMap = productService.getProductMapByIds(productIds);

        Set<Long> brandIds = productMap.values().stream().map(Product::getBrandId).collect(Collectors.toSet());

        Map<Long, ProductMetrics> metricsMap = productMetricsService.getMetricsMapByProductIds(productIds);
        Map<Long, Supply> supplyMap = supplyService.getSupplyMapByProductIds(productIds);
        Map<Long, Brand> brandMap = brandService.getBrandMapByBrandIds(brandIds);

        return likedProducts.map(likeProduct -> {
            Product product = productMap.get(likeProduct.getProductId());
            ProductMetrics metrics = metricsMap.get(product.getId());
            Brand brand = brandMap.get(product.getBrandId());
            Supply supply = supplyMap.get(product.getId());

            return new LikeProductInfo(
                    product.getId(),
                    product.getName(),
                    brand.getName(),
                    product.getPrice().amount(),
                    metrics.getLikeCount(),
                    supply.getStock().quantity()
            );
        });

    }
}
