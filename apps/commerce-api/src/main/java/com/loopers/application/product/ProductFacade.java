package com.loopers.application.product;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.cache.RankingCacheService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final ProductCacheService productCacheService;
    private final RankingCacheService rankingCacheService;

    public List<ProductInfo> getProducts(ProductCommand.GetProductsCommand command) {
        return productCacheService.getProductList(
                command.brandId(),
                command.sort(),
                command.page(),
                command.size(),
                () -> {
                    List<Product> products;

                    if (ProductSort.LATEST.equals(command.sort())) {
                        products = productService.findProductsByLatest(
                                command.brandId(), command.page(), command.size());
                    } else if (ProductSort.PRICE_ASC.equals(command.sort())) {
                        products = productService.findProductsByPriceAsc(
                                command.brandId(), command.page(), command.size());
                    } else if (ProductSort.LIKES_DESC.equals(command.sort())) {
                        products = productService.findProductsByLikesDesc(
                                command.brandId(), command.page(), command.size());
                    } else {
                        throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 정렬 기준입니다: " + command.sort());
                    }

                    Set<Long> brandIds = products.stream()
                            .map(Product::getBrandId)
                            .collect(Collectors.toSet());

                    Map<Long, String> brandNamesMap = brandService.findBrandNamesByIds(brandIds.stream().toList());

                    return products.stream()
                            .map(product -> ProductInfo.from(product, brandNamesMap.get(product.getBrandId())))
                            .toList();
                }
        );
    }

    public ProductInfo getProduct(Long productId) {
        ProductInfo productInfo = productCacheService.getProduct(productId, () -> {
            Product product = productService.findProductById(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
            String brandName = brandService.findBrandNameById(product.getBrandId());
            return ProductInfo.from(product, brandName);
        });

        Long rank = rankingCacheService.getProductRank(LocalDate.now(), productId);

        if (rank != null) {
            return new ProductInfo(
                    productInfo.id(),
                    productInfo.name(),
                    productInfo.brandId(),
                    productInfo.brandName(),
                    productInfo.price(),
                    productInfo.likeCount(),
                    productInfo.stock(),
                    productInfo.createdAt(),
                    rank
            );
        }

        return productInfo;
    }
}

