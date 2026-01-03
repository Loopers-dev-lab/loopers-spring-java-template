package com.loopers.application.ranking;

import com.loopers.application.ranking.RankingInfo.RankingItemInfo;
import com.loopers.application.ranking.RankingInfo.RankingPageInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.ranking.PeriodType;
import com.loopers.domain.ranking.PeriodUtils;
import com.loopers.domain.ranking.RankingService;
import com.loopers.infrastructure.cache.ProductRankingCache;
import com.loopers.infrastructure.cache.ProductRankingCache.RankingEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class RankingFacade {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ProductRankingCache productRankingCache;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final RankingService rankingService;

    /** @param page 0-based */
    public RankingPageInfo getRankings(String date, int page, int size) {
        return getRankings(date, PeriodType.DAILY, page, size);
    }
    
    /** @param page 0-based */
    public RankingPageInfo getRankings(String date, PeriodType periodType, int page, int size) {
        // 날짜 검증 및 변환
        LocalDate targetDate = parseAndValidateDate(date);
        
        return switch (periodType) {
            case DAILY -> getDailyRankings(targetDate, page, size);
            case WEEKLY -> rankingService.getWeeklyRankings(targetDate, page, size);
            case MONTHLY -> rankingService.getMonthlyRankings(targetDate, page, size);
        };
    }
    
    /** @param page 0-based */
    private RankingPageInfo getDailyRankings(LocalDate targetDate, int page, int size) {
        String dateString = targetDate.format(DATE_FORMATTER);

        // 1. ZSET에서 랭킹 조회
        List<RankingEntry> rankingEntries = productRankingCache.getTopRankings(dateString, page, size);

        if (rankingEntries.isEmpty()) {
            return RankingPageInfo.of(Collections.emptyList(), dateString, page, size, 0);
        }

        // 2. 상품 ID 목록 추출
        List<Long> productIds = rankingEntries.stream()
                .map(RankingEntry::productId)
                .toList();

        // 3. 상품 정보 조회
        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 4. 브랜드 정보 조회 (N+1 방지)
        List<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .distinct()
                .toList();
        List<Brand> brands = brandRepository.findByIdIn(brandIds);
        Map<Long, Brand> brandMap = brands.stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));

        // 5. 응답 생성
        List<RankingItemInfo> rankings = rankingEntries.stream()
                .map(entry -> {
                    Product product = productMap.get(entry.productId());
                    if (product == null) {
                        log.warn("[Ranking] Product not found - productId: {}", entry.productId());
                        return null;
                    }
                    Brand brand = brandMap.get(product.getBrandId());
                    String brandName = brand != null ? brand.getName() : "Unknown";

                    return new RankingItemInfo(
                            entry.rank(),
                            product.getId(),
                            product.getName(),
                            brandName,
                            product.getPrice(),
                            product.getLikeCount(),
                            entry.score()
                    );
                })
                .filter(item -> item != null)
                .toList();

        // 6. 전체 개수 조회
        long totalCount = productRankingCache.getTotalCount(dateString);

        return RankingPageInfo.of(rankings, dateString, page, size, totalCount);
    }

    /** @throws IllegalArgumentException 유효하지 않은 날짜 형식 */
    private LocalDate parseAndValidateDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyyMMdd, got: " + date);
        }
    }
    
    /** @throws IllegalArgumentException 유효하지 않은 날짜 형식 */
    private String validateAndNormalizeDate(String date) {
        if (date == null || date.isBlank()) {
            return productRankingCache.getTodayDate();
        }

        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyyMMdd, got: " + date);
        }
    }
}
