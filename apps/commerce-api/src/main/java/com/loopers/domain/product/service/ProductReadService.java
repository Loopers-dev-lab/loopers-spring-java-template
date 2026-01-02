package com.loopers.domain.product.service;

import com.loopers.application.product.CursorPageInfo;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductSummaryInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.like.service.LikeReadService;
import com.loopers.domain.product.command.ProductSearchFilter;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.enums.ProductSortCondition;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.infrastructure.product.ProductRepositoryImpl;
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

    /**
     * 상품 상세 정보 조회 (공통 데이터 + 유저별 좋아요 여부 포함)
     */
    public ProductDetailInfo getProductDetail(Long productId, Long memberIdOrNull) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));

        boolean isLikedByMember = likeReadService.isLikedBy(memberIdOrNull, productId);

        return ProductDetailInfo.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brandId(product.getBrandId())
                .brandName(brand.getName())
                .brandDescription(brand.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .likeCount(product.getLikeCount())
                .isLikedByMember(isLikedByMember)
                .ranking(null)  // 캐시 저장용, Facade에서 실시간 조회로 교체
                .build();
    }

    public Page<ProductSummaryInfo> getProducts(ProductSearchFilter filter, Pageable pageable, Long memberIdOrNull) {
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

    public CursorPageInfo<ProductSummaryInfo> getProductsByCursor(ProductSearchFilter filter, String cursor, int size, Long memberIdOrNull) {
        List<Product> products = productRepository.findAllByCursor(filter, cursor, size);

        boolean hasNext = products.size() > size;
        List<Product> content = hasNext ? products.subList(0, size) : products;

        if (content.isEmpty()) {
            return CursorPageInfo.of(Collections.emptyList(), null, false);
        }

        // Batch fetch brands
        List<Long> brandIds = content.stream()
                .map(Product::getBrandId)
                .distinct()
                .toList();

        List<Brand> brands = brandRepository.findByIdIn(brandIds);
        Map<Long, Brand> brandMap = brands.stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));

        // Batch fetch likes
        Set<Long> likedProductIds = Collections.emptySet();
        if (memberIdOrNull != null) {
            List<Long> productIds = content.stream()
                    .map(Product::getId)
                    .toList();
            likedProductIds = likeReadService.findLikedProductIds(memberIdOrNull, productIds);
        }

        final Set<Long> finalLikedProductIds = likedProductIds;
        List<ProductSummaryInfo> productInfos = content.stream()
                .map(product -> {
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
                })
                .toList();

        // Generate next cursor
        String nextCursor = hasNext
                ? ProductRepositoryImpl.encodeCursor(content.get(content.size() - 1), filter.getSortCondition())
                : null;

        return CursorPageInfo.of(productInfos, nextCursor, hasNext);
    }

    public List<ProductSummaryInfo> getPopularProducts(Long memberIdOrNull) {
        List<Product> products = productRepository.findTopByLikeCount(100);

        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch fetch brands
        List<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .distinct()
                .toList();

        List<Brand> brands = brandRepository.findByIdIn(brandIds);
        Map<Long, Brand> brandMap = brands.stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));

        // Batch fetch likes
        Set<Long> likedProductIds = Collections.emptySet();
        if (memberIdOrNull != null) {
            List<Long> productIds = products.stream()
                    .map(Product::getId)
                    .toList();
            likedProductIds = likeReadService.findLikedProductIds(memberIdOrNull, productIds);
        }

        final Set<Long> finalLikedProductIds = likedProductIds;
        return products.stream()
                .map(product -> {
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
                })
                .toList();
    }

    public List<ProductSummaryInfo> getBrandPopularProducts(Long brandId, int limit, Long memberIdOrNull) {
        // 브랜드 존재 여부 확인
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));

        List<Product> products = productRepository.findTopByBrandIdAndLikeCount(brandId, limit);

        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch fetch likes
        Set<Long> likedProductIds = Collections.emptySet();
        if (memberIdOrNull != null) {
            List<Long> productIds = products.stream()
                    .map(Product::getId)
                    .toList();
            likedProductIds = likeReadService.findLikedProductIds(memberIdOrNull, productIds);
        }

        final Set<Long> finalLikedProductIds = likedProductIds;
        return products.stream()
                .map(product -> {
                    boolean isLikedByMember = finalLikedProductIds.contains(product.getId());

                    return ProductSummaryInfo.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .brandName(brand.getName())
                            .price(product.getPrice())
                            .likeCount(product.getLikeCount())
                            .isLikedByMember(isLikedByMember)
                            .build();
                })
                .toList();
    }

}
