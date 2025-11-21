package com.loopers.domain.product.service;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductSummaryInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.like.service.LikeReadService;
import com.loopers.domain.product.command.ProductSearchFilter;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class ProductReadService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final LikeReadService likeReadService;

    public ProductDetailInfo getProductDetail(Long productId, String memberIdOrNull) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));

        boolean isLikedByMember = likeReadService.isLikedBy(memberIdOrNull, productId);

        return ProductDetailInfo.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brandName(brand.getName())
                .brandDescription(brand.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .likeCount(product.getLikeCount())
                .isLikedByMember(isLikedByMember)
                .build();
    }

    public Page<ProductSummaryInfo> getProducts(ProductSearchFilter filter, Pageable pageable, String memberIdOrNull) {
        Page<Product> products = productRepository.findAll(filter, pageable);
        
        if (products.isEmpty()) {
            return products.map(product -> null);
        }

        // Batch fetch brands to avoid N+1 query
        List<Long> brandIds = products.getContent().stream()
                .map(Product::getBrandId)
                .distinct()
                .toList();
        
        List<Brand> brands = brandRepository.findByIdIn(brandIds);
        Map<Long, Brand> brandMap = brands.stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));

        // Batch fetch likes to avoid N+1 query  
        Set<Long> likedProductIds = Collections.emptySet();
        if (memberIdOrNull != null) {
            List<Long> productIds = products.getContent().stream()
                    .map(Product::getId)
                    .toList();
            likedProductIds = likeReadService.findLikedProductIds(memberIdOrNull, productIds);
        }

        final Set<Long> finalLikedProductIds = likedProductIds;
        return products.map(product -> {
            Brand brand = brandMap.get(product.getBrandId());
            if (brand == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.");
            }

            boolean isLikedByMember = finalLikedProductIds.contains(product.getId());
            
            return ProductSummaryInfo.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .brandName(brand.getName())
                    .price(product.getPrice())
                    .likeCount(product.getLikeCount())
                    .isLikedByMember(isLikedByMember)
                    .build();
        });
    }
}
