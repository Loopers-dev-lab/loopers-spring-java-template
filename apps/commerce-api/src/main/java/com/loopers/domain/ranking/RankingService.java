package com.loopers.domain.ranking;

import com.loopers.application.ranking.RankingInfo.RankingItemInfo;
import com.loopers.application.ranking.RankingInfo.RankingPageInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.ranking.monthly.MonthlyRanking;
import com.loopers.domain.ranking.monthly.MonthlyRankingRepository;
import com.loopers.domain.ranking.weekly.WeeklyRanking;
import com.loopers.domain.ranking.weekly.WeeklyRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {
    
    private final WeeklyRankingRepository weeklyRankingRepository;
    private final MonthlyRankingRepository monthlyRankingRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    
    /**
     * 주간 랭킹 조회
     */
    public RankingPageInfo getWeeklyRankings(LocalDate targetDate, int page, int size) {
        LocalDate weekStartDate = PeriodUtils.getWeekStartDate(targetDate);
        
        // 1. 주간 랭킹 데이터 조회
        List<WeeklyRanking> weeklyRankings = weeklyRankingRepository
            .findByWeekStartDateWithPagination(weekStartDate, page * size, size);
        
        if (weeklyRankings.isEmpty()) {
            return RankingPageInfo.of(Collections.emptyList(), 
                targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), 
                page, size, 0);
        }
        
        // 2. 상품 및 브랜드 정보 조회
        List<RankingItemInfo> rankingItems = buildRankingItemsFromWeekly(weeklyRankings);
        
        // 3. 전체 개수 조회
        List<WeeklyRanking> allRankings = weeklyRankingRepository
            .findByWeekStartDateOrderByRankPosition(weekStartDate);
        long totalCount = allRankings.size();
        
        return RankingPageInfo.of(rankingItems, 
            targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")), 
            page, size, totalCount);
    }
    
    /**
     * 월간 랭킹 조회
     */
    public RankingPageInfo getMonthlyRankings(LocalDate targetDate, int page, int size) {
        String monthKey = PeriodUtils.getMonthKey(targetDate);
        
        // 1. 월간 랭킹 데이터 조회
        List<MonthlyRanking> monthlyRankings = monthlyRankingRepository
            .findByMonthYearWithPagination(monthKey, page * size, size);
        
        if (monthlyRankings.isEmpty()) {
            return RankingPageInfo.of(Collections.emptyList(),
                targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                page, size, 0);
        }
        
        // 2. 상품 및 브랜드 정보 조회
        List<RankingItemInfo> rankingItems = buildRankingItemsFromMonthly(monthlyRankings);
        
        // 3. 전체 개수 조회
        List<MonthlyRanking> allRankings = monthlyRankingRepository
            .findByMonthYearOrderByRankPosition(monthKey);
        long totalCount = allRankings.size();
        
        return RankingPageInfo.of(rankingItems,
            targetDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
            page, size, totalCount);
    }
    
    /**
     * WeeklyRanking을 RankingItemInfo로 변환
     */
    private List<RankingItemInfo> buildRankingItemsFromWeekly(List<WeeklyRanking> weeklyRankings) {
        // 1. 상품 ID 목록 추출
        List<Long> productIds = weeklyRankings.stream()
            .map(WeeklyRanking::getProductId)
            .toList();
        
        // 2. 상품 정보 조회
        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        
        // 3. 브랜드 정보 조회 (N+1 방지)
        List<Long> brandIds = products.stream()
            .map(Product::getBrandId)
            .distinct()
            .toList();
        List<Brand> brands = brandRepository.findByIdIn(brandIds);
        Map<Long, Brand> brandMap = brands.stream()
            .collect(Collectors.toMap(Brand::getId, Function.identity()));
        
        // 4. 응답 생성
        return weeklyRankings.stream()
            .map(ranking -> {
                Product product = productMap.get(ranking.getProductId());
                if (product == null) {
                    log.warn("[WeeklyRanking] Product not found - productId: {}", ranking.getProductId());
                    return null;
                }
                Brand brand = brandMap.get(product.getBrandId());
                String brandName = brand != null ? brand.getName() : "Unknown";
                
                return new RankingItemInfo(
                    ranking.getRankPosition(),
                    product.getId(),
                    product.getName(),
                    brandName,
                    product.getPrice(),
                    product.getLikeCount(),
                    ranking.getTotalScore()
                );
            })
            .filter(item -> item != null)
            .toList();
    }
    
    /**
     * MonthlyRanking을 RankingItemInfo로 변환
     */
    private List<RankingItemInfo> buildRankingItemsFromMonthly(List<MonthlyRanking> monthlyRankings) {
        // 1. 상품 ID 목록 추출
        List<Long> productIds = monthlyRankings.stream()
            .map(MonthlyRanking::getProductId)
            .toList();
        
        // 2. 상품 정보 조회
        List<Product> products = productRepository.findByIdIn(productIds);
        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        
        // 3. 브랜드 정보 조회 (N+1 방지)
        List<Long> brandIds = products.stream()
            .map(Product::getBrandId)
            .distinct()
            .toList();
        List<Brand> brands = brandRepository.findByIdIn(brandIds);
        Map<Long, Brand> brandMap = brands.stream()
            .collect(Collectors.toMap(Brand::getId, Function.identity()));
        
        // 4. 응답 생성
        return monthlyRankings.stream()
            .map(ranking -> {
                Product product = productMap.get(ranking.getProductId());
                if (product == null) {
                    log.warn("[MonthlyRanking] Product not found - productId: {}", ranking.getProductId());
                    return null;
                }
                Brand brand = brandMap.get(product.getBrandId());
                String brandName = brand != null ? brand.getName() : "Unknown";
                
                return new RankingItemInfo(
                    ranking.getRankPosition(),
                    product.getId(),
                    product.getName(),
                    brandName,
                    product.getPrice(),
                    product.getLikeCount(),
                    ranking.getTotalScore()
                );
            })
            .filter(item -> item != null)
            .toList();
    }
}